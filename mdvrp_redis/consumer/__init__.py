import json
import logging
import os
import time
from datetime import datetime
from optparse import OptionParser
from threading import Thread

from redis import Redis

formatter = logging.Formatter('%(asctime)s %(levelname)s %(name)s %(message)s')


class Enumerator(Thread):
    def __init__(self, en, broker_url, out_dir, use_timestamp=False):
        Thread.__init__(self)
        self.props = en
        self.redis = Redis(broker_url)
        self.out_dir = os.path.abspath(out_dir)

        if use_timestamp:
            now = datetime.now().replace(microsecond=0).isoformat()
            self.out_dir = os.path.join(self.out_dir, now)

        try:
            os.makedirs(self.out_dir)
        except:
            pass

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
        self.distances = json.loads(self.redis.get('distances'))
        self.locations = json.loads(self.redis.get('locations'))

        self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])
        self.dump('{} {}'.format(self.props['depot'], '{}'.format(int(time.time() * 1000))))

        while True:
            # Subscribe
            if self.redis.llen('{}.next-routes'.format(self.props['id'])) == 0:
                self.redis.rpush('request', json.dumps((self.props['id'], self.props['depot'])))

            # Wait for any message
            channel, routes = self.redis.blpop('{}.next-routes'.format(self.props['id']))
            if not routes.upper() == 'EOF':
                routes = json.loads(routes)

                # Set 1st route as assigned
                tobe_visited = routes[0]
                if str(self.props['depot']) != tobe_visited['id']:
                    self.redis.set('{}.assigned'.format(tobe_visited['id']), True)

                    try:
                        duration = float(self.distances[str(self.props['depot'])][tobe_visited['id']]['duration']) / 100
                        self.logger.debug('{}-{} duration {} seconds'.format(self.props['depot'], tobe_visited['id'], duration))
                        time.sleep(duration)
                    except Exception, e:
                        pass

                    self.logger.debug('{} visit {}'.format(self.props['id'], tobe_visited))

                    self.props['depot'] = tobe_visited['id']
                    self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])
                    self.dump('{} {}'.format(self.props['depot'], '{}'.format(int(time.time() * 1000))))

                    service_time = self.locations[tobe_visited['id']]['service_time'] / 1000
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
    parser.add_option("-B", "--broker-url",
                      dest="B", default=None,
                      help='Redis broker URL')

    (options, args) = parser.parse_args()
    options = vars(options)

    out_dir = options['O']
    broker_url = options['B']
    use_timestamp = options['t']

    redis = Redis(broker_url)
    enumerators = redis.get('enumerators')

    ens = []
    for _, en in json.loads(enumerators).items():
        en = Enumerator(en, broker_url, out_dir, use_timestamp)
        ens.append(en)

    for en in ens: en.start()
    for en in ens: en.join()


if __name__ == '__main__':
    main()
