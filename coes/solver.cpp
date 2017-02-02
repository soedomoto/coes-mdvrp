//
// Created by soedomoto on 31/01/17.
//

#include "solver.h"
#include "mdvrp_es_coevol.hpp"

Solver::Solver() {}

Solver::~Solver() {}

int const Solver::solve() {
    config->setParameters(problem);

    printf("=======================================================================================\n");
    printf("CoES -- A Cooperative Coevolutionary Algorithm for Multi-Depot Vehicle Routing Problems\n");

    if (config->getProcessType() == Enum_Process_Type::MONO_THREAD)
        printf("* MONO");
    else
        printf("*** MULTI");

    printf(" Thread version\n");
    printf("=======================================================================================\n\n");

    if (config->isDebug()) {
        printf("D\tE\tB\tU\tG\tversion!\n\n");
        printf("=======================================================================================\n\n");
    }

    printf("Instance....: %s\n", problem->getInstance().c_str());
    printf("Depots......: %d\n", problem->getDepots());
    printf("Vehicles....: %d\n", problem->getVehicles());
    printf("Capacity....: %d\n", problem->getCapacity());
    printf("Duration....: %.2f\n", problem->getDuration());
    printf("Customers...: %d\n", problem->getCustomers());

    if (config->getStopCriteria() == NUM_GER)
        printf("Generations....: %lu\n", config->getNumGen());
    else {
        printf("Max time....: %.2f secs. ", config->getExecutionTime());

        if (config->getMaxTimeWithoutUpdate() > 0 &&
            config->getMaxTimeWithoutUpdate() <= config->getExecutionTime())
            printf("(or %.2f secs. no update)", config->getMaxTimeWithoutUpdate());
    }

    printf("\n====Factors for evaluation====\n");
    printf("Tam Subpop..: %d\n", config->getNumSubIndDepots());
    //printf("Lambda......: %d\n", config->getNumOffspringsPerParent());

    printf("Max time...: %.2f seg. ", config->getExecutionTime());
    printf("(or %.2f secs. no update)\n", config->getMaxTimeWithoutUpdate());

    printf("PLS Rate....: %.2f\n", config->getMutationRatePLS());
    printf("Elite Size..: %d\n", config->getEliteGroupLimit());
    printf("==============================\n\n");

    printf("Local Search.: %s\n", config->getLocalSearchType() == RANDOM ? "BLA" : "BLS");
    cout << "\n\n";

    ESCoevolMDVRP esCoevolMDVRP = ESCoevolMDVRP(problem, config);
    esCoevolMDVRP.run();

    time(&end);

    double dif = difftime(end, start);
    printf("\n\nProcesso finalizado. Tempo gasto = %lf\n\n", dif);
    printf("Encerramento: ");
    Util::printTimeNow();

    delete problem;
    delete config;

    return 0;
}

MDVRPProblem *Solver::getProblem() const {
    return problem;
}

AlgorithmConfig *Solver::getConfig() const {
    return config;
}

time_t Solver::getStart() const {
    return start;
}

void Solver::setStart(time_t start) {
    Solver::start = start;
}

time_t Solver::getEnd() const {
    return end;
}

void Solver::setEnd(time_t end) {
    Solver::end = end;
}
