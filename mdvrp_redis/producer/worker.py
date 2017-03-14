import json
import os
import subprocess
import time
from collections import OrderedDict
from threading import Thread

from redis import Redis

from .model import ResultEnumerator


class VRPWorker(Thread):
    is_eof = False
    result_cache = {}

    def __init__(self, app, outdir):
        Thread.__init__(self)

        self.app = app
        self.redis = Redis(app.broker_url)
        self.outdir = outdir

    def watch_channel(self):
        while not self.is_eof:
            _, message = self.redis.blpop('request')
            vehicle, depot = json.loads(message)
            vehicle, depot = str(vehicle), str(depot)

            if self.redis.llen('{}.next-routes'.format(vehicle)) != 0:
                continue

            class Listener(CoESVRPSolverListener):
                is_solved = False

                def __init__(self, worker):
                    self.worker = worker

                def on_started(self, vehicle):
                    self.vehicle = vehicle

                def on_solution(self, vehicle, routes):
                    if self.worker.redis.llen('{}.next-routes'.format(vehicle)) == 0:
                        for i in range(len(routes)): routes[i] = routes[i].clone()
                        self.worker.redis.rpush('{}.next-routes'.format(vehicle), routes)

                    if not self.is_solved and self.vehicle == vehicle:
                        self.is_solved = True

                def on_finished(self, vehicle):
                    if not self.is_solved:
                        sol = CoESVRPSolver(self.worker.app, vehicle, depot, self.worker.outdir, Listener(self.worker),
                                            use_all_vehicle=False)
                        sol.start()
                        sol.join()

                def on_eof(self, vehicle):
                    self.worker.redis.rpush('{}.next-routes'.format(vehicle), 'EOF')
                    self.worker.is_eof = True

            sol = CoESVRPSolver(self.app, vehicle, depot, self.outdir, Listener(self))
            sol.start()
            sol.join()

    def run(self):
        self.watch_channel()


class CoESVRPSolverListener():
    def on_started(self, vehicle):
        pass

    def on_solution(self, vehicle, routes):
        pass

    def on_finished(self, vehicle):
        pass

    def on_eof(self, vehicle):
        pass


class CoESVRPSolver(Thread):
    def __init__(self, app, vehicle, depot, outdir, listener, use_all_vehicle=True):
        Thread.__init__(self)

        self.app = app
        self.redis = Redis(app.broker_url)
        self.vehicle = vehicle
        self.depot = depot
        self.outdir = outdir
        self.listener = listener
        self.use_all_vehicle = use_all_vehicle

    def run(self):
        self.listener.on_started(self.vehicle)

        # List all vehicles and locations
        all_vehicles = OrderedDict()
        for e in self.app.all_enumerators:
            all_vehicles[e.id] = e
        all_locations = OrderedDict()
        for l in self.app.all_bses:
            all_locations[l.id] = l

        # Filter enumerators = all or individu
        self.enumerators = OrderedDict()
        if self.use_all_vehicle:
            self.enumerators = all_vehicles
        else:
            self.enumerators = {self.vehicle: all_vehicles[self.vehicle]}

        # Update depot of each enumerators
        for e in self.enumerators:
            depot = self.redis.get('{}.depot'.format(e)) or e
            if depot in all_locations:
                self.enumerators[e].depot = all_locations[depot].id
                self.enumerators[e].lat = all_locations[depot].lat
                self.enumerators[e].lon = all_locations[depot].lon

        # Filter locations = remove assigned
        assigned_locations = self.redis.keys('*.assigned') or '[]'
        assigned_locations = [l.replace('.assigned', '') for l in assigned_locations]

        self.locations = OrderedDict()
        for l in all_locations:
            if l not in assigned_locations:
                self.locations[l] = all_locations[l]

        if len(self.locations) > 0:
            ts = '{}'.format(int(time.time() * 1000))
            problem_file = os.path.join(self.outdir, ts, 'problem')
            initial_cost_file = os.path.join(self.outdir, ts, 'initial-cost')
            solution_file = os.path.join(self.outdir, ts, 'solution')
            routes_file = os.path.join(self.outdir, ts, 'routes')

            try: os.makedirs(os.path.join(self.outdir, ts))
            except: pass

            self.create_problem_file(problem_file, initial_cost_file)
            retval = subprocess.check_call([self.app.coes_bin, "--depot-subpop-ind", "3", "--max-exec-time", "60",
                                            "--max-time-no-update", "40", problem_file, initial_cost_file, solution_file])
            if retval == 0:
                lines = []
                result_enumerators = self.translate_solution(solution_file)
                for en in result_enumerators:
                    if len(en.routes) > 0:
                        self.listener.on_solution(en.id, en.routes)
                        lines.append('{}\t{}'.format(en.id, ','.join([r.id for r in en.routes])))

                # Backup to file
                with open(routes_file, 'wb') as f:
                    f.write('\n'.join(lines))

            self.listener.on_finished(self.vehicle)

        else:
            for v in all_vehicles:
                self.listener.on_eof(v)

    def translate_solution(self, solution_file):
        lines = open(solution_file).readlines()

        enumeratorIdxs = lines[0].strip().split()
        routeIdxs = lines[1].strip().split()
        routeCosts = lines[2].strip().split()
        routeDemands = lines[3].strip().split()
        strDestinationBsIdxs = lines[4].strip().split()
        destinationBsIdxs = []

        for s in strDestinationBsIdxs:
            destinationBsIdxs.append(s.strip().split(','))

        routeProps = zip(enumeratorIdxs, routeIdxs, routeCosts, routeDemands, destinationBsIdxs)

        result_enumerators = [ResultEnumerator(e) for e in self.enumerators.values()]
        for en_id, ro_id, cost, demand, routes in routeProps:
            result_enumerators[int(en_id)].total_costs += float(cost)
            result_enumerators[int(en_id)].total_demands += int(demand)
            routes = [self.locations.values()[int(r)-1] for r in routes]
            result_enumerators[int(en_id)].routes = result_enumerators[int(en_id)].routes + routes

        return result_enumerators

    def create_problem_file(self, problem_file, initial_cost_file):
        lines = []
        lines.append('{} {} {} {}'.format(2, len(self.enumerators), len(self.locations), len(self.enumerators)))

        for i in self.enumerators:
            e = self.enumerators[i]
            lines.append('{} {}'.format(0.0, int(len(self.locations) / len(self.enumerators)) + 1))

        l = 1
        for i in self.locations:
            b = self.locations[i]
            lines.append('{} {} {} {} {} {} {} {} {} {} {}'.format(l, b.lon, b.lat, 0, b.demand, 1, 4, 1, 2, 4, 8))
            l += 1

        for i in self.enumerators:
            e = self.enumerators[i]
            lines.append('{} {} {} {} {} {} {}'.format(l, e.lon, e.lat, 0, 0, 0, 0))
            l += 1

        for a in self.locations:
            durations = []
            for b in self.locations:
                durations.append(str(self.app.distance_matrix[a][b]['duration']))
            lines.append(' '.join(durations))
            l += 1

        for a in self.enumerators:
            durations = []
            for b in self.locations:
                durations.append(str(self.app.distance_matrix[str(self.enumerators[a].depot)][b]['duration']))
            lines.append(' '.join(durations))
            l += 1

        with open(problem_file, 'wb') as f:
            f.write('\n'.join(lines))

        with open(initial_cost_file, 'wb') as f:
            f.write('1\n')