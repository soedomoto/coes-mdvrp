import json
import os
import math
import random
import subprocess
from collections import OrderedDict

import numpy
from rediscluster import RedisCluster
import matplotlib.pyplot as plt

instance = 'p05'
conf = subprocess.check_output('docker inspect cordeau_{0}_notw_redis_cluster_1'.format(instance).split())
conf = json.loads(conf)[0]
nx = conf['NetworkSettings']
redis_host = nx['IPAddress']

cache = RedisCluster(host=redis_host, port=6379)
distances = json.loads(cache.get('distances'))

from mdvrp_redis.producer import CordeauFile

cf = CordeauFile().read_file(instance)
enumerators = {}
for e in cf.all_enumerators:
    enumerators[e.id] = e.__dict__
locations = {}
for e in cf.all_bses:
    locations[e.id] = e.__dict__


def stdev(lst):
    num_items = len(lst)
    mean = sum(lst) / num_items
    differences = [x - mean for x in lst]
    sq_differences = [d ** 2 for d in differences]
    ssd = sum(sq_differences)
    variance = ssd / num_items
    sd = math.sqrt(variance)
    return sd

def plot_coes(verts):
    fig = plt.figure()

    for seri in verts:
        ax = fig.add_subplot(111)
        xs, ys = zip(*seri)
        ax.plot(xs, ys, 'o--', lw=1, color=numpy.random.rand(3, 1), ms=10)

    plt.show()


def plot(verts, name='plot'):
    from matplotlib import colors as mcolors

    colors = dict(mcolors.TABLEAU_COLORS)
    if len(verts) > len(colors):
        colors = dict(mcolors.BASE_COLORS, **mcolors.CSS4_COLORS)
    # Sort colors by hue, saturation, value and name.
    by_hsv = sorted((tuple(mcolors.rgb_to_hsv(mcolors.to_rgba(color)[:3])), name)
                    for name, color in colors.items())
    color_names = [n for hsv, n in by_hsv]

    import networkx as nx
    G = nx.DiGraph()

    all_ns = {}
    depots = []
    v_customers = {}
    first_edges = []
    v_edges = {}
    node_labels = {}
    for v, ns in verts.items():
        v_edges.setdefault(v, [])
        depots.append(ns.keys()[0])
        v_customers[v] = list(ns.keys()[1:])
        all_ns.update(ns)

        for n in ns.keys():
            node_labels[n] = n

        first_edges.append((ns.keys()[0], ns.keys()[1]))
        for i in range(2, len(ns.keys())):
            v_edges[v].append((ns.keys()[i - 1], ns.keys()[i]))

    colors = []
    for i in range(0, len(v_edges)):
        i = i * len(color_names)/len(v_edges)
        colors.append(color_names[i])

    nx.draw_networkx_nodes(G, all_ns, nodelist=depots, node_color='r', node_size=500, alpha=0.8, node_shape='s',
                           label='Enumerator')
    nx.draw_networkx_nodes(G, all_ns, nodelist=[c for customers in v_customers.values() for c in customers],
                           node_color='b', node_size=200, alpha=1, label='Location')
    nx.draw_networkx_labels(G, all_ns, labels=node_labels, font_size=8)

    nx.draw_networkx_edges(G, all_ns, edgelist=first_edges, edge_color='r', alpha=1, width=1, label='First Route')
    for i, (v, edges) in enumerate(v_edges.items()):
        nx.draw_networkx_edges(G, all_ns, edgelist=edges, edge_color=colors[i], alpha=1, width=1,
                               label='Route of {0}'.format(v))

    plt.xlabel('Longitude')
    plt.ylabel('Latitude')
    lgd = plt.legend(loc='upper left', bbox_to_anchor=(1, 1))
    # plt.show()
    plt.savefig(name, bbox_extra_artists=(lgd,), bbox_inches='tight')
    plt.clf()


def coes():
    root = 'producer'
    log_dirs = os.listdir(root)
    log_dirs = [d for d in log_dirs if not d.startswith('.')]
    log_dirs = sorted(log_dirs)
    dir = log_dirs[0]

    l_routes = []
    vst_locs = []
    route_costs = {}
    verts = OrderedDict()
    file = os.path.join(root, dir, 'routes')
    for line in open(file).readlines():
        id, routes = line.strip().split()
        routes = routes.strip().split(',')

        l_route = []
        seri = OrderedDict()
        seri[id] = (enumerators[id]['lon'], enumerators[id]['lat'])
        cost = 0
        for li in range(len(routes)):
            if li > 0:
                a, b = routes[li - 1], routes[li]
            else:
                a, b = id, routes[li]

            duration = distances[a][b]['duration']
            service_time = locations[b]['service_time']
            cost += duration + service_time

            l_route.append(b)
            vst_locs.append(b)
            seri[routes[li]] = (locations[routes[li]]['lon'], locations[routes[li]]['lat'])

        route_costs[id] = cost
        verts[id] = seri
        l_routes.append('{} & {} & {} \\\\'.format(id, '-'.join(l_route), cost))

    print 'CoES Only'
    print '========='
    print 'Location count:', len(vst_locs)
    print 'Total costs:', "%.2f" % sum(route_costs.values())
    print 'Rata-rata:', "%.2f" % (sum(route_costs.values()) / len(route_costs.values()))
    print 'Std dev:', "%.2f" % stdev(route_costs.values())
    print ''

    print 'Latex Table'
    print '-----------'
    print '\n'.join(l_routes)
    print ''

    plot(verts, name='test_result_cordeau_{0}_notw_pubsub_coes'.format(instance))


def pubsub_coes():
    for root, dirs, files in os.walk('consumer'):
        l_routes = []
        vst_locs = []
        route_costs = {}
        verts = OrderedDict()
        for file in files:
            file = os.path.join(root, file)
            if file.endswith('.res'):
                routes = []
                lines = list(open(file).readlines())
                for line in lines:
                    id, ts = line.strip().split()
                    id, ts = id, int(ts)
                    routes.append(id)

                l_route = []
                seri = OrderedDict()
                seri[routes[0]] = (enumerators[routes[0]]['lon'], enumerators[routes[0]]['lat'])

                cost = 0
                for li in range(len(routes))[1:]:
                    duration = distances[routes[li - 1]][routes[li]]['duration']
                    service_time = locations[routes[li]]['service_time']
                    cost += duration + service_time

                    l_route.append(routes[li])
                    vst_locs.append(routes[li])
                    seri[routes[li]] = (locations[routes[li]]['lon'], locations[routes[li]]['lat'])

                route_costs[routes[0]] = cost
                l_routes.append('{} & {} & {} \\\\'.format(routes[0], '-'.join(l_route), cost))
                verts[routes[0]] = seri

        print 'CoES Pub/Sub'
        print '============'
        print 'Location count:', len(vst_locs)
        print 'Total costs:', "%.2f" % sum(route_costs.values())
        print 'Rata-rata:', "%.2f" % (sum(route_costs.values()) / len(route_costs.values()))
        print 'Std dev:', "%.2f" % stdev(route_costs.values())
        print ''

        print 'Latex Table'
        print '-----------'
        print '\n'.join(l_routes)
        print ''

        plot(verts, name='test_result_cordeau_{0}_notw_coes'.format(instance))


def main():
    coes()
    pubsub_coes()


if __name__ == '__main__':
    main()
