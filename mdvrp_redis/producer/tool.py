import json
import random
import threading
import math
from .model import Enumerator, CensusBlock, CostMatrix


class CordeauFile(object):
    def __init__(self):
        self.distance_matrix = {}
        self.all_bses = []
        self.all_enumerators = []

    def write_file(self, path, cost_path=None):
        lines = []

        lines.append('{} {} {} {}'.format(2, len(self.all_enumerators), len(self.all_bses), len(self.all_enumerators)))

        for e in self.all_enumerators:
            lines.append('{} {}'.format(e.duration, e.capacity))

        line_no = 1
        for bs in self.all_bses:
            lines.append('{} {} {} {} {} {} {} {} {} {} {}'.format(line_no, bs.lon, bs.lat, bs.service_time,
                                                                   bs.demand, 1, 4, 1, 2, 4, 8))
            line_no += 1

        for e in self.all_enumerators:
            lines.append('{} {} {} {} {} {} {}'.format(line_no, e.lon, e.lat, 0, 0, 0, 0))
            line_no += 1

        with open(path, 'wb') as f:
            f.write('\n'.join(lines))

        if cost_path:
            dt = []
            for a, bd in self.distance_matrix.items():
                for b, dm in bd.items():
                    dt.append('{} {} {} {}'.format(a, b, dm['distance'], dm['duration']))

            with open(cost_path, 'wb') as f:
                f.write('\n'.join(dt))


    def read_file(self, path, cost_path=None):
        cost_matrix = {}
        if cost_path:
            for line in open(cost_path).readlines():
                a, b, c, d = line.strip().split()
                cost_matrix.setdefault(a, {})
                cost_matrix[a][b] = {
                    'distance': float(c),
                    'duration': float(d)
                }

        def distance_duration(t, f):
            if t.id in cost_matrix and f.id in cost_matrix[t.id]:
                return cost_matrix[t.id][f.id]['distance'], cost_matrix[t.id][f.id]['duration']

            return math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2)), \
                   math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))


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
            b.service_time = float(idx_x_y__d[3])
            b.demand = int(idx_x_y__d[4])
            self.all_bses.append(b)

            l += 1

        for i in range(nDepot):
            idx_x_y = lines[l].strip().split()
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
                    # cm.distance = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    # cm.duration = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    cm.distance, cm.duration = distance_duration(f, t)
                else:
                    cm.distance = float(0)
                    cm.duration = float(0)
                self.distance_matrix.setdefault(f.id, {})
                self.distance_matrix[f.id][t.id] = json.loads(str(cm))

        for f in self.all_enumerators:
            for t in self.all_bses:
                cm = CostMatrix()
                cm.fromm = f.id
                cm.to = t.id
                if f.id != t.id:
                    # cm.distance = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    # cm.duration = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    cm.distance, cm.duration = distance_duration(f, t)
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
                    # cm.distance = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    # cm.duration = math.sqrt(pow(t.lon-f.lon, 2) + pow(t.lat-f.lat, 2))
                    cm.distance, cm.duration = distance_duration(f, t)
                else:
                    cm.distance = float(0)
                    cm.duration = float(0)
                self.distance_matrix.setdefault(f.id, {})
                self.distance_matrix[f.id][t.id] = json.loads(str(cm))

        return self


def random_service_time():
    ruta_count = 10;
    lambda_interview_time = 1800
    # Based on (Sudman, 1965) : inter-node within segment = 15% time, interview = 37% time
    lambda_internode_time = (15.0 / 37) * ((ruta_count - 1) / ruta_count) * lambda_interview_time
    std_dev = 0.25 * lambda_internode_time

    ruta_service_times = 0.0
    for i in range(ruta_count):
        t = random.gauss(lambda_interview_time, 0.25 * lambda_interview_time)
        ruta_service_times += t

    inter_rute_times = 0.0
    for i in range(ruta_count):
        t = random.gauss(lambda_internode_time, 0.25 * lambda_internode_time)
        inter_rute_times += t

    segment_service_time = ruta_service_times + inter_rute_times
    return segment_service_time


class CountDownLatch(object):
    def __init__(self, count=1):
        self.count = count
        self.lock = threading.Condition()

    def count_down(self):
        self.lock.acquire()
        self.count -= 1
        if self.count <= 0:
            self.lock.notifyAll()
        self.lock.release()

    def await(self):
        self.lock.acquire()
        while self.count > 0:
            self.lock.wait()
        self.lock.release()