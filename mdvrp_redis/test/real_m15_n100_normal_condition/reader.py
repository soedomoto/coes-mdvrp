import json
import os
import math
from redis import Redis

cache = Redis('172.17.0.4')
locations = json.loads(cache.get('locations'))
distances = json.loads(cache.get('distances'))


def stdev(lst):
    num_items = len(lst)
    mean = sum(lst) / num_items
    differences = [x - mean for x in lst]
    sq_differences = [d ** 2 for d in differences]
    ssd = sum(sq_differences)
    variance = ssd / num_items
    sd = math.sqrt(variance)
    return sd


def coes():
    root = 'producer'
    log_dirs = os.listdir(root)
    log_dirs = sorted(log_dirs)
    dir = log_dirs[0]

    vst_locs = []
    route_costs = {}
    file = os.path.join(root, dir, 'routes')
    for line in open(file).readlines():
        id, routes = line.strip().split()
        routes = routes.strip().split(',')

        cost = 0
        for li in range(len(routes)):
            if li > 0:
                a, b = routes[li - 1], routes[li]
            else:
                a, b = id, routes[li]

            duration = distances[a][b]['duration']
            service_time = locations[b]['service_time']
            cost += duration + service_time
            vst_locs.append(b)

        route_costs[id] = cost

    print 'CoES Only'
    print '========='
    print 'Location count:', len(vst_locs)
    print 'Total costs:', "%.2f" % sum(route_costs.values())
    print 'Rata-rata:', "%.2f" % (sum(route_costs.values()) / len(route_costs.values()))
    print 'Std dev:', "%.2f" % stdev(route_costs.values())
    print ''


def pubsub_coes():
    for root, dirs, files in os.walk('consumer'):
        vst_locs = []
        route_costs = {}
        for file in files:
            file = os.path.join(root, file)
            if file.endswith('.res'):
                routes = []
                lines = list(open(file).readlines())
                for line in lines:
                    id, ts = line.strip().split()
                    id, ts = id, int(ts)
                    routes.append(id)

                cost = 0
                for li in range(len(routes))[1:]:
                    duration = distances[routes[li - 1]][routes[li]]['duration']
                    service_time = locations[routes[li]]['service_time']
                    cost += duration + service_time

                    vst_locs.append(routes[li])

                route_costs[routes[0]] = cost

        print 'CoES Pub/Sub'
        print '============'
        print 'Location count:', len(vst_locs)
        print 'Total costs:', "%.2f" % sum(route_costs.values())
        print 'Rata-rata:', "%.2f" % (sum(route_costs.values()) / len(route_costs.values()))
        print 'Std dev:', "%.2f" % stdev(route_costs.values())
        print ''


def main():
    coes()
    pubsub_coes()


if __name__ == '__main__':
    main()
