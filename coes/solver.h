//
// Created by soedomoto on 31/01/17.
//

#ifndef LIB_COES_SOLVER_H
#define LIB_COES_SOLVER_H


#include "mdvrp_problem.hpp"
#include "algorithm_config.hpp"

class Solver {
    time_t start, end;
    MDVRPProblem *problem = new MDVRPProblem();
    AlgorithmConfig *config = new AlgorithmConfig();
public:
    Solver();

    virtual ~Solver();

    int const solve();

    MDVRPProblem *getProblem() const;

    AlgorithmConfig *getConfig() const;

    time_t getStart() const;

    void setStart(time_t start);

    time_t getEnd() const;

    void setEnd(time_t end);
};


#endif //LIB_COES_SOLVER_H
