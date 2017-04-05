import json
import logging
import os
import random
import socket
import time
from datetime import datetime
from multiprocessing import Process
from optparse import OptionParser
from threading import Thread, Lock
import select
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

        self.locations = None
        self.simulation_time_scale = None
        self.distances = None
        self.sigma_delay = None
        self.mu_delay = None
        self.broker_url = broker_url
        self.use_proxy = use_proxy
        self.proxy_port = proxy_port
        self.props = en
        self.out_dir = os.path.abspath(out_dir)

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

        self.redis = RedisCluster(host=proxy_host, port=proxy_port)

        self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])
        self.dump('{} {}'.format(self.props['depot'], '{}'.format(int(time.time() * 1000))))

        while True:
            # Subscribe
            self.logger.debug('subscribe')

            if self.redis.llen('{}.next-routes'.format(self.props['id'])) == 0:
                self.redis.rpush('request', json.dumps((self.props['id'], self.props['depot'])))

            # Wait for any message
            channel, routes = self.redis.blpop('{}.next-routes'.format(self.props['id']))
            if not routes.upper() == 'EOF':
                routes = json.loads(routes)

                # Set 1st route as assigned
                tobe_visited = routes[0]
                if str(self.props['depot']) != tobe_visited['id'] or self.props['id'] == str(self.props['depot']):
                    self.redis.set('{}.received'.format(tobe_visited['id']), self.props['id'])

                    # Transportation process
                    try:
                        duration = float(self.distances[str(self.props['depot'])][tobe_visited['id']]['duration']) * self.simulation_time_scale
                        self.logger.debug('transport {}->{} for {} s'.format(self.props['depot'], tobe_visited['id'], duration))
                        time.sleep(duration)
                    except Exception, e:
                        pass

                    self.logger.debug('arrived to {}'.format(tobe_visited['id']))

                    self.props['depot'] = tobe_visited['id']
                    self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])
                    self.dump('{} {}'.format(self.props['depot'], '{}'.format(int(time.time() * 1000))))

                    # Enumeration process
                    service_time = self.locations[tobe_visited['id']]['service_time'] * self.simulation_time_scale
                    self.logger.debug('enumerating {} for {} s'.format(self.props['depot'], service_time))
                    time.sleep(service_time)

                    # Geographical delay
                    if self.mu_delay:
                        geo_delay = random.gauss(self.mu_delay, self.sigma_delay) * self.simulation_time_scale
                        self.logger.debug('geographical delay for {} s'.format(geo_delay))
                        self.delay_writer.debug('{} {}'.format(self.props['depot'], geo_delay))
                        time.sleep(geo_delay)

            else:
                break


def main():
    parser = OptionParser()
    parser.set_description('Run consumer simulator')
    parser.set_usage(parser.get_usage().replace('\n', ''))
    parser.add_option("-O", "--output-dir",
                      dest="O", default="None",
                      help='Output directory')
    parser.add_option("-D", "--data",
                      dest="D", default=None,
                      help="Data problem file")
    parser.add_option("-C", "--cost-file",
                      dest="C", default=None,
                      help="Cost matrix file")
    parser.add_option("-S", "--time-scale",
                      dest="S", default=float(1)/100, type=float,
                      help="Scale simulation time to real time")
    parser.add_option("-t", "--use-timestamp",
                      action="store_true", dest="t", default=False,
                      help='Append timestamp to output directory')
    parser.add_option("-p", "--use-proxy",
                      action="store_true", dest="p", default=False,
                      help='Use auto proxy server')
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
    cost_file = options['C']
    out_dir = options['O']
    broker_urls = options['B']
    use_timestamp = options['t']
    use_proxy = options['p']
    simulation_time_scale = options['S']
    mu_delay = options['m']
    sigma_delay = options['s']

    cf = CordeauFile().read_file(problem_file, cost_file)
    enumerators = cf.all_enumerators
    distances = cf.distance_matrix
    locations = {l.id: l.__dict__ for l in cf.all_bses}

    if use_proxy:
        proxy_ports = get_open_port(n=len(enumerators))

    ens = []
    i = 0
    for e in enumerators:
        en = Enumerator(e.__dict__, broker_urls[random.randint(0, len(broker_urls)-1)], out_dir, use_timestamp,
                        use_proxy, proxy_ports[i] if use_proxy else None)
        en.distances = distances
        en.locations = locations
        en.simulation_time_scale = simulation_time_scale
        en.mu_delay = mu_delay
        en.sigma_delay = sigma_delay
        ens.append(en)
        i += 1

    for en in ens: en.start()
    for en in ens: en.join()


if __name__ == '__main__':
    main()
