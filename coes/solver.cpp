#include "solver.h"
#include <string>

solver::solver() {
    //ctor
}

solver::~solver() {
    //dtor
}

int const solver::solve() {
    MDVRPProblem *problem = new MDVRPProblem();

    time_t start, end;

    char inst[10], dat[300], sol[300];

    // FATORES A SEREM AVALIADOS
    bool change = false;

    int popSize;
    float executionTime;
    float maxTimeWithoutUpdate;
    float mutationRatePLS;
    int eliteGroupLimit;

    strcpy(dat, BASE_DIR_DAT);
    strcpy(sol, BASE_DIR_SOL);

    strcat(dat, INST_TEST);
    strcat(sol, INST_TEST);
    strcpy(inst, INST_TEST);

//    if (argc == 1) {
//        strcat(dat, INST_TEST);
//        strcat(sol, INST_TEST);
//        strcpy(inst, INST_TEST);
//    } else {
//        strcat(dat, argv[1]);
//        strcat(sol, argv[1]);
//        strcpy(inst, argv[1]);
//
//        if (argc > 2) {
//
//            change = true;
//
//            popSize = atoi(argv[2]);
//            executionTime = strtof(argv[3], NULL);
//            maxTimeWithoutUpdate = strtof(argv[4], NULL);
//            mutationRatePLS = strtof(argv[5], NULL);
//            eliteGroupLimit = atoi(argv[6]);
//
//        }
//    }

    strcat(sol, ".res");

    time(&start);

    /* initialize random seed: */
    Random::randomize();

    // Read data file
    if (!problem->processInstanceFiles(dat, sol, inst)) {
        //std::cout << "Press enter to continue ...";
        //std::cin.get();
        return 1;
    }

    // Generation of data allocation - allocation N / M
    //problem->printAllocation();
    problem->printAllocationDependecy();
    // return 0;

    // # Settings for execution
    AlgorithmConfig *config = new AlgorithmConfig();
    config->setParameters(problem);

    if (change) {

        config->setNumSubIndDepots(popSize);
        config->setExecutionTime(executionTime);
        config->setMaxTimeWithoutUpdate(maxTimeWithoutUpdate);
        config->setMutationRatePLS(mutationRatePLS);
        config->setEliteGroupLimit(eliteGroupLimit);

        config->setWriteFactors(true);
    }

    //printf("INSTANCIA...: %s\n", problem->getInstCode().c_str());
    //problem->print();
    //LocalSearch::testFunction(problem, config);
    //return(0);


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

        if (!change) {
            printf("Max time....: %.2f secs. ", config->getExecutionTime());

            if (config->getMaxTimeWithoutUpdate() > 0 &&
                config->getMaxTimeWithoutUpdate() <= config->getExecutionTime())
                printf("(or %.2f secs. no update)", config->getMaxTimeWithoutUpdate());

            printf("\n");
        }
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
