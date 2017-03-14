import json
from threading import Thread
import time
from redis import Redis


class DataCache(Thread):
    def __init__(self, app, latch):
        Thread.__init__(self)

        self.app = app
        self.redis = Redis(app.broker_url)
        self.latch = latch

    def cache_location(self, initial=False):
        if not initial:
            pass

        self.redis.delete('locations')
        self.redis.set('locations', json.dumps({l.id: l.__dict__ for l in self.app.all_bses}))

    def cache_enumerator(self, initial=False):
        if not initial:
            pass

        self.redis.delete('enumerators')
        self.redis.set('enumerators', json.dumps({e.id: e.__dict__ for e in self.app.all_enumerators}))

    def cache_distance_matrix(self, initial=False):
        if not initial:
            pass

        self.redis.delete('distances')
        self.redis.set('distances', json.dumps(self.app.distance_matrix))

    def run(self):
        delay = 0
        initial = True
        while True:
            self.cache_location(initial)
            self.cache_enumerator(initial)
            self.cache_distance_matrix(initial)

            self.latch.count_down()

            time.sleep(delay)

            delay = 60
            initial = False
