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

    def __init__(self, (proxy_host, proxy_port), (dest_host, dest_port), logfile=None):
        Process.__init__(self)

        self.dest_host = dest_host
        self.dest_port = dest_port

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
                self.proxy_delay = random.uniform(0.0, 5.0)  # use random 0-20 seconds
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
        self.cache = RedisCluster(host=self.broker_url, port=6379, readonly_mode=True)
        self.distances = json.loads(self.cache.get('distances'))
        self.locations = json.loads(self.cache.get('locations'))

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

                    try:
                        duration = float(self.distances[str(self.props['depot'])][tobe_visited['id']]['duration']) * 10 / 1000
                        self.logger.debug('{}-{} duration {} seconds'.format(self.props['depot'], tobe_visited['id'], duration))
                        time.sleep(duration)
                    except Exception, e:
                        pass

                    self.logger.debug('{} visit {}'.format(self.props['id'], tobe_visited))

                    self.props['depot'] = tobe_visited['id']
                    self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])
                    self.dump('{} {}'.format(self.props['depot'], '{}'.format(int(time.time() * 1000))))

                    service_time = self.locations[tobe_visited['id']]['service_time'] * 10 / 1000
                    self.logger.debug('{} seconds'.format(service_time))
                    time.sleep(service_time)

            else:
                break


def main():
    parser = OptionParser()
    parser.set_description('Run location recommendation server')
    parser.set_usage(parser.get_usage().replace('\n', ''))
    parser.add_option("-O", "--output-dir",
                      dest="O", default="None",
                      help='Output directory')
    parser.add_option("-t", "--use-timestamp",
                      action="store_true", dest="t", default=False,
                      help='Append timestamp to output directory')
    parser.add_option("-p", "--use-proxy",
                      action="store_true", dest="p", default=False,
                      help='Use auto proxy server')
    parser.add_option("-B", "--broker-url",
                      dest="B", default=[], action="append",
                      help='Redis broker URL(s)')

    (options, args) = parser.parse_args()
    options = vars(options)

    out_dir = options['O']
    broker_urls = options['B']
    use_timestamp = options['t']
    use_proxy = options['p']

    redis = RedisCluster(host=broker_urls[random.randint(0, len(broker_urls)-1)], port=6379)
    enumerators = redis.get('enumerators')

    proxy_ports = get_open_port(n=len(enumerators))

    ens = []
    i = 0
    for _, en in json.loads(enumerators).items():
        en = Enumerator(en, broker_urls[random.randint(0, len(broker_urls)-1)], out_dir, use_timestamp,
                        use_proxy, proxy_ports[i])
        ens.append(en)
        i += 1

    for en in ens: en.start()
    for en in ens: en.join()


if __name__ == '__main__':
    main()
