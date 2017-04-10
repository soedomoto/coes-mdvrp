import json
import os
import random
import subprocess
import time
from threading import Thread
from mdvrp_redis.producer import CordeauFile
from mdvrp_redis.producer import random_service_time


# ======================================================================================================================
# Configuration
# ======================================================================================================================
app_name = os.path.basename(os.path.dirname(__file__))
data_file = 'm15_n182'
simulation_time_scale = float(1)/100

# Kondisi relatif homogen, art sedikit, dengan geografi mudah
# Geographical delay
# Normal delay 5 menit +- 3 menit sebelum melakukan subscription
mu_delay = 5 * 60
sigma_delay = 3 * 60

# rata-rata wawancara 1 ruta = 27,58 menit
# stdev wawancara 1 ruta = 4.16 menit
lambda_interview_time = 27.58 * 60
sigma_interview_time = 4.16 * 60
# ======================================================================================================================


def setup_redis_cluster_docker():
    cont_names = ['{}_redis_cluster_{}'.format(app_name, i) for i in range(1, 7)]
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

    docker_exec = 'docker exec {} /bin/sh -c "echo yes | /redis-trib.rb create --replicas 1 {}"'.format(
        cont_names[0], ' '.join(brokers))
    try: print subprocess.check_output(docker_exec, shell=True)
    except Exception, e: print e

    return [b.split(':')[0] for b in brokers]


def prepare_data():
    if not os.path.exists(data_file):
        cf = CordeauFile().read_file('m15_n182_base')
        for i in range(len(cf.all_bses)):
            cf.all_bses[i].service_time = random_service_time(lambda_interview_time=lambda_interview_time,
                                                              sigma_interview_time=sigma_interview_time)
        cf.write_file(data_file)


class Producer(Thread):
    def __init__(self, brokers):
        Thread.__init__(self)
        self.brokers = brokers

    def run(self):
        retval = subprocess.check_call(
            ['../../bin/mdvrp-redis-producer',
             '-b', '/media/soedomoto/DATA/ITB2015/EL5090 - Research Method/Research/Dynamic Enumerator Allocation/App/jni-coes-mdvrp/coes_mdvrp_bin',
             '-B', self.brokers[random.randint(0, len(self.brokers)-1)],
             '-D', os.path.abspath(os.path.join(os.getcwd(), data_file)),
             '-C', os.path.abspath(os.path.join(os.getcwd(), './distance_duration_table')),
             '-O', os.path.abspath(os.path.join(os.getcwd(), './producer')),
             '-X', str(30)
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
             '-D', os.path.abspath(os.path.join(os.getcwd(), data_file)),
             '-C', os.path.abspath(os.path.join(os.getcwd(), './distance_duration_table')),
             '-O', os.path.abspath(os.path.join(os.getcwd(), './consumer')),
             '-S', str(simulation_time_scale),
             '-m', str(mu_delay),
             '-s', str(sigma_delay)
             ])
        print 'Consumer retval {}'.format(retval)


def main():
    prepare_data()
    brokers = setup_redis_cluster_docker()

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