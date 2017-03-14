import json


class Location(object):
    id = None
    lat = None
    lon = None

    def clone(self):
        l = Location()
        l.id = self.id
        l.lat = self.lat
        l.lon = self.lon
        return l

    def __repr__(self):
        return json.dumps(self.__dict__)


class Enumerator(Location):
    depot = None
    duration = -1
    capacity = -1

    def __repr__(self):
        return json.dumps(self.__dict__)


class CensusBlock(Location):
    service_time = None
    assigned_to = None
    assigned_date = None
    visited_by = None
    visited_date = None
    demand = 1

    def __repr__(self):
        return json.dumps(self.__dict__)


class CostMatrix(object):
    fromm = None
    to = None
    distance = None
    duration = None

    def __repr__(self):
        this = self.__dict__.copy()
        try:
            this['from'] = this['fromm']
            this.pop('fromm')
        except: pass

        return json.dumps(this)

class ResultEnumerator(Enumerator):
    total_costs = 0.0
    total_demands = 0
    routes = []

    def __init__(self, parent):
        Enumerator.__init__(self)
        self.id = parent.id
        self.lat = parent.lat
        self.lon = parent.lon
        self.depot = parent.depot
        self.duration = parent.duration
        self.capacity = parent.capacity
