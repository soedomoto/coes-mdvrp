import random
import threading


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