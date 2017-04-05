import logging
import os
from datetime import datetime
from optparse import OptionParser

from mdvrp_redis.producer.cache import DataCache
from mdvrp_redis.producer.model import Enumerator, CensusBlock, CostMatrix
from mdvrp_redis.producer.tool import random_service_time, CountDownLatch, CordeauFile
from mdvrp_redis.producer.worker import VRPWorker


formatter = logging.Formatter('%(asctime)s %(levelname)s %(name)s %(message)s')


class Producer(object):
    all_enumerators = []
    all_bses = []
    distance_matrix = {}

    def __init__(self, options):

        self.coes_bin = options['bin']
        if not os.path.isabs(self.coes_bin): self.coes_bin = os.path.abspath(self.coes_bin)

        self.broker_url = options['B']
        self.out_dir = os.getcwd()
        if options['O']:
            self.out_dir = options['O']
        if not os.path.isabs(self.out_dir):
            self.out_dir = os.path.abspath(self.out_dir)
        if options['t']:
            self.out_dir = os.path.join(self.out_dir, datetime.now().replace(microsecond=0).isoformat())
        if options['X']:
            self.execution_time = options['X']

        try: os.makedirs(self.out_dir)
        except: pass

        log_file = os.path.join(self.out_dir, 'app.log')
        self.logger = logging.getLogger('app')
        fileHandler = logging.FileHandler(log_file)
        fileHandler.setFormatter(formatter)
        self.logger.addHandler(fileHandler)
        stdoutHandler = logging.StreamHandler()
        stdoutHandler.setFormatter(formatter)
        self.logger.addHandler(stdoutHandler)
        self.logger.setLevel(logging.DEBUG)

        DATA_PATH = options['D']
        COST_PATH = options['C']
        if COST_PATH:
            cf = CordeauFile().read_file(DATA_PATH, COST_PATH)
        else:
            cf = CordeauFile().read_file(DATA_PATH)

        self.all_bses = cf.all_bses
        self.all_enumerators = cf.all_enumerators
        self.distance_matrix = cf.distance_matrix

        latch = CountDownLatch()
        DataCache(self, latch).start()

        latch.await()
        VRPWorker(self).start()

    def start(self):
        pass


def main():
    parser = OptionParser()
    parser.set_description('Run location recommendation server')
    parser.set_usage(parser.get_usage().replace('\n', ''))
    parser.add_option("-b", "--coes-bin",
                      dest="bin", default=None,
                      help="Binary of CoES MDVRP library")
    parser.add_option("-D", "--data",
                      dest="D", default=None,
                      help="Data problem file")
    parser.add_option("-C", "--cost-file",
                      dest="C", default=None,
                      help="Cost matrix file")
    parser.add_option("-O", "--output-dir",
                      dest="O", default="None",
                      help='Output directory')
    parser.add_option("-t", "--use-timestamp",
                      action="store_true", dest="t", default=False,
                      help='Append timestamp to output directory')
    parser.add_option("-B", "--broker-url",
                      dest="B", default=None,
                      help='Redis broker URL')
    parser.add_option("-X", "--solver-execution-time",
                      dest="X", default=60, type=int,
                      help='VRP Solver execution time')

    (options, args) = parser.parse_args()
    options = vars(options)

    if len(args) != 0:
        parser.print_help()
        exit()

    if not options['bin'] or not options['D'] or not options['O'] or not options['B']:
        parser.print_help()
        exit()

    Producer(options).start()


if __name__ == '__main__':
    main()
