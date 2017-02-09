#include <elite_group.hpp>
#include <community.hpp>
#include "com_soedomoto_vrp_solver_CoESVRPJNI.h"

jint vSize;
jfloat* jvXDepots;
jfloat* jvYDepots;
jfloat* jvDurations;
jint* jvCapacities;
jint cSize;
jfloat* jcXDepots;
jfloat* jcYDepots;
jint* jcDemands;

JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setVehicles
        (JNIEnv *env, jobject, jint size, jfloatArray xDepots, jfloatArray yDepots, jfloatArray durations, jintArray capacities) {
    vSize = size;

    jvXDepots = env->GetFloatArrayElements(xDepots ,0);
    jvYDepots = env->GetFloatArrayElements(yDepots ,0);
    jvDurations = env->GetFloatArrayElements(durations ,0);
    jvCapacities = env->GetIntArrayElements(capacities ,0);
}

JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setCustomers
        (JNIEnv *env, jobject, jint size, jfloatArray xDepots, jfloatArray yDepots, jintArray demands) {
    cSize = size;

    jcXDepots = env->GetFloatArrayElements(xDepots ,0);
    jcYDepots = env->GetFloatArrayElements(yDepots ,0);
    jcDemands = env->GetIntArrayElements(demands ,0);
}

JNIEXPORT jint JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_solve
        (JNIEnv *env, jobject) {
    MDVRPProblem *problem = new MDVRPProblem();
    AlgorithmConfig *config = new AlgorithmConfig();

    // Solver *solver = new Solver();
    problem->setInstCode("java");
    problem->setInstance("java");
    problem->setDepots(vSize);
    problem->setVehicles(vSize);
    problem->setCustomers(cSize);

    problem->allocateMemory();
    problem->setBestKnowSolution(1.0);

    for (int i = 0; i < problem->getDepots(); ++i) {
//        problem->setDuration(jvDurations[i]);
//        problem->setCapacity(jvCapacities[i]);

        problem->getDurations().at(i) = jvDurations[i];
        problem->getCapacities().at(i) = jvCapacities[i];

        typedef_point point;
        point.x = jvXDepots[i];
        point.y = jvYDepots[i];

        problem->getDepotPoints().at(i) = point;
    }

    for (int i = 0; i < problem->getCustomers(); ++i) {
        typedef_point point;
        point.x = jcXDepots[i];
        point.y = jcYDepots[i];

        problem->getCustomerPoints().at(i) = point;
        problem->getDemand().at(i) = jcDemands[i];
    }

    problem->calculateMatrixDistance();
    problem->setNearestCustomersFromCustomer();
    problem->setNearestCustomersFromDepot();
    problem->setNearestDepotsFromCustomer();
    problem->defineIntialCustomersAllocation();
    problem->operateGranularNeighborhood();

    config->setParameters(problem);
    config->setProcessType(MONO_THREAD);

    // Print configuration
    printf("=======================================================================================\n");
    printf("CoES -- A Cooperative Coevolutionary Algorithm for Multi-Depot Vehicle Routing Problems\n");

    if (config->getProcessType() == Enum_Process_Type::MONO_THREAD) printf("* MONO");
    else printf("*** MULTI");

    printf(" Thread version\n");
    printf("=======================================================================================\n\n");

    if (config->isDebug()) {
        printf("D\tE\tB\tU\tG\tversion!\n\n");
        printf("=======================================================================================\n\n");
    }

    printf("Instance....: %s\n", problem->getInstance().c_str());
    printf("Depots......: %d\n", problem->getDepots());
    printf("Vehicles....: %d\n", problem->getVehicles());
    printf("Capacity....: %d\n", problem->getCapacities().at(0));
    printf("Duration....: %.2f\n", problem->getDurations().at(0));
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
    fflush(stdout);

    // Instantiate variables
    time_t start;
    time(&start);
    problem->getMonitor().setStart(start);
    EliteGroup *eliteGroup = new EliteGroup(problem, config);
    problem->getMonitor().createLocks(problem->getDepots(), config->getNumSubIndDepots());
    Community *community = new Community(problem, config, eliteGroup);
    community->pairingRandomly();
    community->evaluateSubpops(true);
    community->pairingAllVsBest();
    community->evaluateSubpops(true);

    community->printEvolution();
    community->getProblem()->getMonitor().updateGeneration();

    // ##### Start manager ###### ----------------
    try {
        community->manager();
    }
    catch (exception &e) {
        cout << e.what();
    }
    // ########################## ----------------

    // Print final solution
    community->getEliteGroup()->getBest().printSolution();

    cout << "\nCustomers: " << community->getEliteGroup()->getBest().getNumTotalCustomers() << endl;
    cout << "Route Customers: " << community->getEliteGroup()->getBest().getNumTotalCustomersFromRoutes() << endl
         << endl;

    community->getEliteGroup()->printValues();

    //community->printSubpopList();

    // Clear memory
    delete community;

    return 0;
}