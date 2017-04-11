import json
import logging
import os
import random
import socket
import threading
import time
from datetime import datetime
from multiprocessing import Process
from optparse import OptionParser
from threading import Thread, Lock
import select

import sys

import math
from redis import Redis
from rediscluster import RedisCluster

from mdvrp_redis.producer import CordeauFile

formatter = logging.Formatter('%(asctime)s %(levelname)s %(name)s %(message)s')


def get_open_port(n=1):
    sockets = []
    ports = []
    for _ in range(n):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(("", 0))
        s.listen(1)
        port = s.getsockname()[1]
        sockets.append(s)
        ports.append(port)

    for s in sockets: s.close()

    return ports if n > 1 else ports[0]


class ProxyServer(Process):
    lock = Lock()

    # Changing the buffer_size and delay, you can improve the speed and bandwidth.
    # But when buffer get to high or delay go too down, you can broke things
    buffer_size = 4096

    input_list = []
    channel = {}

    def __init__(self, (proxy_host, proxy_port), (dest_host, dest_port), delay_range=(0.0, 1.0), logfile=None):
        Process.__init__(self)

        self.dest_host = dest_host
        self.dest_port = dest_port
        self.delay_range = delay_range

        self.logger = logging.getLogger('Proxy-{}:{}-{}:{}'.format(proxy_host, proxy_port, dest_host, dest_port))
        if logfile:
            fileHandler = logging.FileHandler(logfile)
            fileHandler.setFormatter(formatter)
            self.logger.addHandler(fileHandler)
        stdoutHandler = logging.StreamHandler()
        stdoutHandler.setFormatter(formatter)
        self.logger.addHandler(stdoutHandler)
        self.logger.setLevel(logging.DEBUG)

        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server.bind((proxy_host, proxy_port))
        self.server.listen(200)

    def set_buffer_size(self, buffer_size):
        self.buffer_size = buffer_size

    def run(self):
        Thread(target=self.release_lock).start()
        self.main_loop()

    def release_lock(self):
        while True:
            if self.lock.locked():
                self.proxy_delay = random.uniform(*self.delay_range)  # use random 0-20 seconds
                self.logger.debug('Delay from {} seconds'.format(self.proxy_delay))

                time.sleep(self.proxy_delay)

                try:
                    self.lock.release()
                except Exception, e:
                    self.logger.error(e.message)

    def main_loop(self):
        self.input_list.append(self.server)
        while True:
            ss = select.select
            inputready, outputready, exceptready = ss(self.input_list, [], [])
            for self.s in inputready:
                if self.s == self.server:
                    self.on_accept()
                    break

                self.data = self.s.recv(self.buffer_size)
                if len(self.data) == 0:
                    self.on_close()
                    break
                else:
                    self.on_recv()

    def on_accept(self):
        self.lock.acquire()

        is_forwarded = False
        forward = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            forward.connect((self.dest_host, self.dest_port))
            is_forwarded = True
        except Exception, e:
            print e

        clientsock, clientaddr = self.server.accept()
        if is_forwarded:
            self.logger.debug('{} is connected'.format(clientaddr))

            self.input_list.append(clientsock)
            self.input_list.append(forward)
            self.channel[clientsock] = forward
            self.channel[forward] = clientsock
        else:
            self.logger.debug('Can\'t establish connection with remote server. '
                              'Closing connection with client side {}'.format(clientaddr))
            clientsock.close()

    def on_close(self):
        self.lock.acquire()

        self.logger.debug('{} is connected'.format(self.s.getpeername()))

        #remove objects from input_list
        self.input_list.remove(self.s)
        self.input_list.remove(self.channel[self.s])
        out = self.channel[self.s]
        # close the connection with client
        self.channel[out].close()  # equivalent to do self.s.close()
        # close the connection with remote server
        self.channel[self.s].close()
        # delete both objects from channel dict
        del self.channel[out]
        del self.channel[self.s]

    def on_recv(self):
        self.lock.acquire()

        data = self.data
        # here we can parse and/or modify the data before send forward
        self.logger.debug(data)
        self.channel[self.s].send(data)


class Enumerator(Thread):
    def __init__(self, en, broker_url, out_dir, use_timestamp=False, use_proxy=False, proxy_port=None):
        Thread.__init__(self)

        self.lock_file = None
        self.start_time = None
        self.location_received = False
        self.explicit_quit = False
        self.quit_after = None
        self.locations = None
        self.simulation_time_scale = None
        self.distances = None
        self.consumer_delays = None
        self.sigma_delay = None
        self.mu_delay = None
        self.broker_url = broker_url
        self.use_proxy = use_proxy
        self.proxy_port = proxy_port
        self.props = en
        self.out_dir = os.path.abspath(out_dir)
        self.all_visited = []

        if use_timestamp:
            now = datetime.now().replace(microsecond=0).isoformat()
            self.out_dir = os.path.join(self.out_dir, now)

        try: os.makedirs(self.out_dir)
        except: pass

        log_file = os.path.join(self.out_dir, self.props['id'] + '.log')
        self.logger = logging.getLogger(str(self.props['id']))
        fileHandler = logging.FileHandler(log_file)
        fileHandler.setFormatter(formatter)
        self.logger.addHandler(fileHandler)
        stdoutHandler = logging.StreamHandler()
        stdoutHandler.setFormatter(formatter)
        self.logger.addHandler(stdoutHandler)
        self.logger.setLevel(logging.DEBUG)

        log_file = os.path.join(self.out_dir, self.props['id'] + '.delay')
        self.delay_writer = logging.getLogger(str(self.props['id']) + '-delay')
        fileHandler = logging.FileHandler(log_file)
        fileHandler.setFormatter(logging.Formatter('%(message)s'))
        self.delay_writer.addHandler(fileHandler)
        self.delay_writer.setLevel(logging.DEBUG)

    def dump(self, str):
        res_file = os.path.join(self.out_dir, self.props['id'] + '.res')

        if os.path.exists(res_file):
            f = open(res_file, 'a')
        else:
            f = open(res_file, 'wb')
            f.write('')

        f.write(str + '\n')
        f.flush()
        f.close()

    def is_quit(self, has_location_received=False):
        self.location_received = has_location_received

        if self.quit_after:
            if self.current_time > self.start_time + self.quit_after * self.simulation_time_scale:
                self.explicit_quit = True
                return True

        return False

    def check_enumerator_time_window(self, est_transport_to_next_loc):
        # TIme starts from 8 am, so hour=0 equal to 8 oclock
        # Time window for service 8 - 16 => 0 - 8 => 0 - 479 mins
        day_current_time = self.current_time % 1440
        # Estimated next location = av
        prev_service_times = [b for a,b in self.all_visited]
        est_next_service_time = int(sum(prev_service_times) / len(prev_service_times))
        est_finish_service = day_current_time + est_transport_to_next_loc + est_next_service_time

        self.logger.debug('Transporting to and enumerating next location will pass enumerator time windows. '
                          'Job will delayed until tomorrow')

        if est_finish_service > 479:
            self.delay(1440 - day_current_time)


    def check_lock(self):
        if len(self.redis.keys('.lock')) > 0:
            time.sleep(1)
            return self.check_lock()
        return False

    def delay(self, delay):
        for t in range(delay):
            self.check_lock()
            self.current_time += 1

    def run(self):
        if self.use_proxy:
            proxy_host = '127.0.0.1'
            proxy_port = self.proxy_port
            proxy_log_file = os.path.join(self.out_dir, self.props['id'] + '-proxy.log')
            ProxyServer((proxy_host, proxy_port), (self.broker_url, 6379), logfile=proxy_log_file).start()
            time.sleep(5)
        else:
            proxy_host = self.broker_url
            proxy_port = 6379

        # self.redis = RedisCluster(host=proxy_host, port=proxy_port)
        self.redis = Redis(host=proxy_host, port=proxy_port)

        self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])
        self.dump('{} {}'.format(self.props['depot'], '{}'.format(int(time.time() * 1000))))

        # self.start_time = time.time()
        self.start_time = 0
        self.current_time = 0

        tobe_visited = None
        while True:
            if self.is_quit(): break
            self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])

            # Subscribe
            self.logger.debug('subscribe')

            if self.redis.llen('{}.next-routes'.format(self.props['id'])) == 0:
                self.redis.rpush('request', json.dumps((str(self.props['id']), str(self.props['depot']))))

            # Wait for any message
            message = self.redis.blpop('{}.next-routes'.format(self.props['id']), 300)
            if not message: continue
            channel, routes = message
            if not routes: continue
            if not routes.upper() == 'EOF':
                routes = json.loads(routes)

                # Set 1st route as assigned
                tobe_visited = routes[0]
                if str(self.props['depot']) != tobe_visited['id'] or self.props['id'] == str(self.props['depot']):
                    self.redis.set('{}.received'.format(tobe_visited['id']), self.props['id'])

                    if self.is_quit(True): break

                    # Transportation process
                    # try:
                    #     duration = float(self.distances[str(self.props['depot'])][tobe_visited['id']]['duration']) * self.simulation_time_scale
                    #     self.logger.debug('transport {}->{} for {} s'.format(self.props['depot'], tobe_visited['id'], duration))
                    #     time.sleep(duration)
                    # except Exception, e:
                    #     pass
                    try:
                        duration = int(self.distances[str(self.props['depot'])][tobe_visited['id']]['duration'] *
                                       self.simulation_time_scale)

                        self.check_enumerator_time_window(duration)

                        self.logger.debug('transport {}->{} for {} s (from {} - {})'.format(self.props['depot'],
                                            tobe_visited['id'], duration, self.current_time, self.current_time + duration))
                        self.delay(duration)
                    except Exception, e:
                        pass

                    if self.is_quit(True): break

                    self.logger.debug('arrived to {}'.format(tobe_visited['id']))

                    self.props['depot'] = tobe_visited['id']
                    self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])
                    self.dump('{} {}'.format(self.props['depot'], '{}'.format(int(time.time() * 1000))))

                    # Enumeration process
                    # service_time = self.locations[tobe_visited['id']]['service_time'] * self.simulation_time_scale
                    # self.logger.debug('enumerating {} for {} s'.format(self.props['depot'], service_time))
                    # time.sleep(service_time)
                    service_time = int(self.locations[tobe_visited['id']]['service_time'] * self.simulation_time_scale)
                    self.logger.debug('enumerating {} for {} s (from {} - {})'.format(self.props['depot'], service_time,
                                        self.current_time, self.current_time + service_time))
                    self.delay(service_time)
                    self.all_visited.append((tobe_visited['id'], service_time))

                    if self.is_quit(): break

                    # Geographical delay
                    if self.consumer_delays:
                        geo_delay = self.consumer_delays[self.props['depot']]
                        self.logger.debug('geographical delay for {} s'.format(geo_delay))
                        self.delay_writer.debug('{} {}'.format(self.props['depot'], geo_delay))
                        # time.sleep(geo_delay)
                        self.delay(geo_delay)

                        if self.is_quit(): break
                    elif self.mu_delay:
                        geo_delay = abs(random.gauss(self.mu_delay, self.sigma_delay) * self.simulation_time_scale)
                        self.logger.debug('geographical delay for {} s'.format(geo_delay))
                        self.delay_writer.debug('{} {}'.format(self.props['depot'], geo_delay))
                        # time.sleep(geo_delay)
                        self.delay(geo_delay)

                        if self.is_quit(): break

            else:
                self.logger.debug('No more locations. Finish.')
                break

        if self.explicit_quit and tobe_visited:
            self.logger.debug('I quit')
            if not self.location_received:
                self.redis.lpop('{}.next-routes'.format(tobe_visited['id']))

            self.redis.delete('{}.received'.format(tobe_visited['id']))


def main():
    parser = OptionParser()
    parser.set_description('Run consumer simulator')
    parser.set_usage(parser.get_usage().replace('\n', ''))
    parser.add_option("-O", "--output-dir",
                      dest="O", default="None",
                      help='Output directory')
    parser.add_option("-L", "--lock-file",
                      dest="L", default="None",
                      help='Output directory')
    parser.add_option("-D", "--data",
                      dest="D", default=None,
                      help="Data problem file")
    parser.add_option("-C", "--cost-file",
                      dest="C", default=None,
                      help="Cost matrix file")
    parser.add_option("-Q", "--quit-list-file",
                      dest="Q", default=None,
                      help="Quit list file")
    parser.add_option("-S", "--time-scale",
                      dest="S", default=float(1)/100, type=float,
                      help="Scale simulation time to real time")
    parser.add_option("-t", "--use-timestamp",
                      action="store_true", dest="t", default=False,
                      help='Append timestamp to output directory')
    parser.add_option("-p", "--use-proxy",
                      action="store_true", dest="p", default=False,
                      help='Use auto proxy server')
    parser.add_option("-d", "--delay-file",
                      dest="d", default=None,
                      help='Delay file')
    parser.add_option("-m", "--mu-delay",
                      dest="m", default=0.0, type=float,
                      help='Mu delay')
    parser.add_option("-s", "--sigma-delay",
                      dest="s", default=0.0, type=float,
                      help='Sigma delay')
    parser.add_option("-B", "--broker-url",
                      dest="B", default=[], action="append",
                      help='Redis broker URL(s)')

    (options, args) = parser.parse_args()
    options = vars(options)

    if len(args) != 0:
        parser.print_help()
        exit()

    if not options['D'] or not options['O'] or not options['B']:
        parser.print_help()
        exit()

    problem_file = options['D']
    lock_file = options['L']
    cost_file = options['C']
    quit_file = options['Q']
    out_dir = options['O']
    broker_urls = options['B']
    use_timestamp = options['t']
    use_proxy = options['p']
    simulation_time_scale = options['S']
    mu_delay = options['m']
    sigma_delay = options['s']
    delay_file = options['d']

    cf = CordeauFile().read_file(problem_file, cost_file)
    enumerators = cf.all_enumerators
    distances = cf.distance_matrix
    locations = {l.id: l.__dict__ for l in cf.all_bses}

    quits = {}
    if quit_file:
        for l in open(quit_file).readlines():
            e, t = l.split()
            quits[e] = float(t)

    consumer_delays = {}
    if quit_file:
        for l in open(delay_file).readlines():
            e, t = l.split()
            consumer_delays[e] = float(t)

    if use_proxy:
        proxy_ports = get_open_port(n=len(enumerators))

    ens = []
    i = 0
    for e in enumerators:
        en = Enumerator(e.__dict__, broker_urls[random.randint(0, len(broker_urls)-1)], out_dir, use_timestamp,
                        use_proxy, proxy_ports[i] if use_proxy else None)
        en.id = e.id
        en.lock_file = lock_file
        en.distances = distances
        en.locations = locations
        en.simulation_time_scale = simulation_time_scale
        en.consumer_delays = consumer_delays
        en.mu_delay = mu_delay
        en.sigma_delay = sigma_delay
        if e.id in quits:
            en.quit_after = quits[e.id]
        ens.append((en.quit_after if en.quit_after else sys.maxint, en))
        i += 1

    ens = sorted(ens)
    e = threading.Event()
    for _, en in ens: en.start()
    for _, en in ens:
        # en.join(en.quit_after)
        # e.set()
        en.join()



if __name__ == '__main__':
    main()