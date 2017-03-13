import json
import sys
from multiprocessing import Process
from threading import Thread

import time
from redis import Redis


class Enumerator(Thread):
    def __init__(self, en, broker_url):
        Thread.__init__(self)
        self.props = en
        self.redis = Redis(broker_url)

    def run(self):
        self.distances = json.loads(self.redis.get('distances'))
        self.locations = json.loads(self.redis.get('locations'))

        self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])

        while True:
            # Subscribe
            if self.redis.llen('{}.next-routes'.format(self.props['id'])) == 0:
                self.redis.rpush('request', json.dumps((self.props['id'], self.props['depot'])))

            # Wait for any message
            channel, routes = self.redis.blpop('{}.next-routes'.format(self.props['id']))
            routes = json.loads(routes)

            # Set 1st route as assigned
            tobe_visited = routes[0]
            if str(self.props['depot']) != tobe_visited['id']:
                self.redis.set('{}.assigned'.format(tobe_visited['id']), True)

                try:
                    duration = float(self.distances[str(self.props['depot'])][tobe_visited['id']]['duration']) / 100
                    print '{}-{} duration {} seconds'.format(self.props['depot'], tobe_visited['id'], duration)
                    time.sleep(duration)
                except Exception, e:
                    pass

                print '{} visit {}'.format(self.props['id'], tobe_visited)

                self.props['depot'] = tobe_visited['id']
                self.redis.set('{}.depot'.format(self.props['id']), self.props['depot'])

                service_time = self.locations[tobe_visited['id']]['service_time'] / 1000
                print '{} seconds'.format(service_time)
                time.sleep(service_time)


if __name__ == '__main__':
    redis = Redis(sys.argv[1])
    enumerators = redis.get('enumerators')

    ens = []
    for _, en in json.loads(enumerators).items():
        en = Enumerator(en, sys.argv[1])
        ens.append(en)

    for en in ens: en.start()
    for en in ens: en.join()