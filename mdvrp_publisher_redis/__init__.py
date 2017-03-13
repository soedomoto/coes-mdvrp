import json
import math
import os
from datetime import datetime
from optparse import OptionParser

import sys

from .cache import DataCache
from .model import Enumerator, CensusBlock, CostMatrix
from .tool import random_service_time, CountDownLatch
from .worker import VRPWorker


class Producer(object):
    all_enumerators = []
    all_bses = []
    distance_matrix = {}

    def __init__(self, options):
        self.coes_bin = options['bin']
        if not os.path.isabs(self.coes_bin): self.coes_bin = os.path.abspath(self.coes_bin)

        self.broker_url = options['B']
        BROKER_URL = options['B']
        DATA_PATH = options['D']

        OUT_DIR = options['O']
        if not os.path.isabs((OUT_DIR)): OUT_DIR = os.path.join(os.getcwd(), OUT_DIR)
        if options['t']: OUT_DIR = os.path.join(OUT_DIR, datetime.now().isoformat())

        self.read_file(DATA_PATH)

        latch = CountDownLatch()
        DataCache(self, BROKER_URL, latch).start()

        latch.await()
        VRPWorker(self, OUT_DIR).start()

    def read_file(self, path):
        lines = open(path).readlines()

        _dcv = lines[0].strip().split()
        nVehicle, nCustomer, nDepot = int(_dcv[1]), int(_dcv[2]), int(_dcv[3])

        l = 1
        for _ in range(nDepot):
            d_c = lines[l].strip().split()
            e = Enumerator()
            e.duration = float(d_c[0])
            e.capacity = int(d_c[1])
            self.all_enumerators.append(e)

            l += 1

        for _ in range(nCustomer):
            idx_x_y__d = lines[l].strip().split()
            b = CensusBlock()
            b.id = idx_x_y__d[0]
            b.lon = float(idx_x_y__d[1])
            b.lat = float(idx_x_y__d[2])
            b.demand = int(idx_x_y__d[4])
            b.service_time = random_service_time()
            self.all_bses.append(b)

            l += 1

        for i in range(nDepot):
            idx_x_y = lines[l].strip().split();
            self.all_enumerators[i].id = idx_x_y[0]
            self.all_enumerators[i].depot = long(idx_x_y[0])
            self.all_enumerators[i].lon = float(idx_x_y[1])
            self.all_enumerators[i].lat = float(idx_x_y[2])

            l += 1

        for f in self.all_bses:
            for t in self.all_bses:
                cm = CostMatrix()
                cm.fromm = f.id
                cm.to = t.id
                if f.id != t.id:
                    cm.distance = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    cm.duration = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                else:
                    cm.distance = 3.4028235E38
                    cm.duration = 3.4028235E38
                self.distance_matrix.setdefault(f.id, {})
                self.distance_matrix[f.id][t.id] = json.loads(str(cm))

        for f in self.all_enumerators:
            for t in self.all_bses:
                cm = CostMatrix()
                cm.fromm = f.id
                cm.to = t.id
                if f.id != t.id:
                    cm.distance = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    cm.duration = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                else:
                    cm.distance = float(0)
                    cm.duration = float(0)
                self.distance_matrix.setdefault(f.id, {})
                self.distance_matrix[f.id][t.id] = json.loads(str(cm))

        for f in self.all_bses:
            for t in self.all_enumerators:
                cm = CostMatrix()
                cm.fromm = f.id
                cm.to = t.id
                if f.id != t.id:
                    cm.distance = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    cm.duration = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                else:
                    cm.distance = float(0)
                    cm.duration = float(0)
                self.distance_matrix.setdefault(f.id, {})
                self.distance_matrix[f.id][t.id] = json.loads(str(cm))

    def start(self):
        pass


def main():
    parser = OptionParser()
    parser.set_description('Run location recommendation server')
    parser.set_usage(parser.get_usage().replace('\n', ''))
    parser.add_option("-b", "--coes-bin",
                      dest="bin", default=None,
                      help="Binary of CoES library")
    parser.add_option("-D", "--data",
                      dest="D", default=None,
                      help="JDBC URL of database / File")
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

    if len(args) != 0:
        parser.print_help()
        exit()

    Producer(options).start()


if __name__ == '__main__':
    main()