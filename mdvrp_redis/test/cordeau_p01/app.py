import os
import subprocess
from multiprocessing import Process

import time
from threading import Thread


class Producer(Thread):
    def __init__(self):
        Thread.__init__(self)

    def run(self):
        retval = subprocess.check_call(
            ['../../bin/mdvrp-redis-producer',
             '-b', '/media/soedomoto/DATA/ITB2015/EL5090 - Research Method/Research/Dynamic Enumerator Allocation/App/jni-coes-mdvrp/coes_mdvrp_bin',
             '-B', '172.17.0.3',
             '-D', os.path.abspath(os.path.join(os.getcwd(), './cordeau_p01')),
             '-O', os.path.abspath(os.path.join(os.getcwd(), './producer'))
             ])
        print 'Producer retval {}'.format(retval)


class Consumer(Thread):
    def __init__(self):
        Thread.__init__(self)

    def run(self):
        retval = subprocess.check_call(
            ['../../bin/mdvrp-redis-consumer',
             '-B', '172.17.0.3',
             '-O', os.path.abspath(os.path.join(os.getcwd(), './consumer'))
             ])
        print 'Consumer retval {}'.format(retval)


def main():
    p = Producer()
    c = Consumer()

    p.start()
    time.sleep(10)
    c.start()

    c.join()
    p.join()

if __name__ == '__main__':
    main()