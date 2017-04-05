import json
import os
import random
import subprocess
from multiprocessing import Process

import time
from threading import Thread

from mdvrp_redis.consumer import get_open_port, ProxyServer


def setup_redis_cluster_docker():
    cont_names = ['m15_n182_packet_loss_redis_cluster_{}'.format(i) for i in range(1, 7)]
    docker_stop = 'docker stop {}'.format(' '.join(cont_names))
    docker_remove = 'docker rm {}'.format(' '.join(cont_names))
    docker_runs = ['docker run -d -P --name {} soedomoto/redis-cluster:latest /redis.conf'.format(n) for n in cont_names]

    try: print subprocess.check_output(docker_stop.split())
    except Exception, e: print e
    try: print subprocess.check_output(docker_remove.split())
    except Exception, e: print e
    for run in docker_runs:
        try: print subprocess.check_output(run.split())
        except Exception, e: print e

    docker_inspects = ['docker inspect {}'.format(n) for n in cont_names]
    brokers = []
    for ins in docker_inspects:
        try:
            conf = subprocess.check_output(ins.split())
            conf = json.loads(conf)[0]
            nx = conf['NetworkSettings']
            brokers.append('{}:{}'.format(nx['IPAddress'], 6379))
        except Exception, e:
            print e

    docker_exec = 'docker exec {} /bin/sh -c "echo yes | /redis-trib.rb create --replicas 1 {}"'.format(cont_names[0], ' '.join(brokers))
    try: print subprocess.check_output(docker_exec, shell=True)
    except Exception, e: print e

    netem_cont_names = ['netem_m15_n182_packet_loss_redis_cluster_{}'.format(i) for i in range(1, 7)]
    netem_docker_stop = 'docker stop {}'.format(' '.join(netem_cont_names))
    netem_docker_remove = 'docker rm {}'.format(' '.join(netem_cont_names))
    netem_docker_runs = [
        'docker run -d --name {} -v /var/run/docker.sock:/var/run/docker.sock gaiaadm/pumba pumba netem ' \
        '--interface eth0 --duration 1440m loss --percent 10 {}'
            .format(netem_cont_names[i], n) for i, n in enumerate(cont_names)]

    try: print subprocess.check_output(netem_docker_stop.split())
    except Exception, e: print e
    try: print subprocess.check_output(netem_docker_remove.split())
    except Exception, e: print e
    for run in netem_docker_runs:
        try: print subprocess.check_output(run.split())
        except Exception, e: print e

    return [b.split(':')[0] for b in brokers]


class Producer(Thread):
    def __init__(self, brokers):
        Thread.__init__(self)
        self.brokers = brokers

    def run(self):
        retval = subprocess.check_call(
            ['../../bin/mdvrp-redis-producer',
             '-b', '/media/soedomoto/DATA/ITB2015/EL5090 - Research Method/Research/Dynamic Enumerator Allocation/App/jni-coes-mdvrp/coes_mdvrp_bin',
             '-B', self.brokers[random.randint(0, len(self.brokers)-1)],
             '-D', os.path.abspath(os.path.join(os.getcwd(), './m15_n182')),
             '-C', os.path.abspath(os.path.join(os.getcwd(), './distance_duration_table')),
             '-O', os.path.abspath(os.path.join(os.getcwd(), './producer'))
             ])
        print 'Producer retval {}'.format(retval)


class Consumer(Thread):
    def __init__(self, brokers):
        Thread.__init__(self)
        self.brokers = brokers

    def run(self):
        retval = subprocess.check_call(
            ['../../bin/mdvrp-redis-consumer',
             '-B', self.brokers[0],
             '-B', self.brokers[1],
             '-B', self.brokers[2],
             '-B', self.brokers[3],
             '-B', self.brokers[4],
             '-B', self.brokers[5],
             '-O', os.path.abspath(os.path.join(os.getcwd(), './consumer')),
             ])
        print 'Consumer retval {}'.format(retval)


def main():
    brokers = setup_redis_cluster_docker()
    print brokers

    time.sleep(5)

    p = Producer(brokers)
    c = Consumer(brokers)

    p.start()
    time.sleep(10)
    c.start()

    c.join()
    p.join()

if __name__ == '__main__':
    main()