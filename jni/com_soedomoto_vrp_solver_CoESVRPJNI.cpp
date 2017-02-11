#include <elite_group.hpp>
#include <community.hpp>
#include "com_soedomoto_vrp_solver_CoESVRPJNI.h"

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

JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setCustomerToCustomerDistance
        (JNIEnv *env, jobject, jint sizeX, jint sizeY, jobjectArray distances) {
    customerDistances = typedef_vectorMatrix<float>(sizeX);
    for (int i = 0; i < sizeX; ++i) customerDistances.at(i).resize(sizeY);

    for (int i = 0; i < sizeX; ++i) {
        jfloatArray rows = (jfloatArray) env->GetObjectArrayElement(distances, i);
        jfloat *row = env->GetFloatArrayElements(rows, 0);
        for (int j = 0; j < sizeY; ++j) {
            customerDistances.at(i).at(j) = row[j];
        }
    }

    explicitC2CDistance = true;
}

JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setDepotToCustomerDistance
        (JNIEnv *env, jobject, jint sizeX, jint sizeY, jobjectArray distances) {
    depotDistances = typedef_vectorMatrix<float>(sizeX);
    for (int i = 0; i < sizeX; ++i) depotDistances.at(i).resize(sizeY);

    for (int i = 0; i < sizeX; ++i) {
        jfloatArray rows = (jfloatArray) env->GetObjectArrayElement(distances, i);
        jfloat *row = env->GetFloatArrayElements(rows, 0);
        for (int j = 0; j < sizeY; ++j) {
            depotDistances.at(i).at(j) = row[j];
        }
    }

    explicitD2CDistance = true;
}

JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_configSetNumSubpopulation
        (JNIEnv *, jobject, jint num) {
    config->setNumSubIndDepots(num);
}

JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_configStopWhenMaxExecutionTime
        (JNIEnv *, jobject, jfloat time) {
    config->setExecutionTime(time);
}

JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_configStopWhenMaxTimeWithoutUpdate
        (JNIEnv *, jobject, jfloat time) {
    config->setMaxTimeWithoutUpdate(time);
}

JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_configStopWhenNumGeneration
        (JNIEnv *, jobject, jint gen) {
    config->setNumGen(gen);
}

JNIEXPORT jintArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionDepots
        (JNIEnv *env, jobject) {
    jintArray jArr = env->NewIntArray(depots.size());
    env->SetIntArrayRegion(jArr, 0, depots.size(), &depots[0]);
    depots.empty();

    return jArr;
}

JNIEXPORT jintArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionRoutes
        (JNIEnv *env, jobject) {
    jintArray jArr = env->NewIntArray(routes.size());
    env->SetIntArrayRegion(jArr, 0, routes.size(), &routes[0]);
    routes.empty();

    return jArr;
}

JNIEXPORT jfloatArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionCosts
        (JNIEnv *env, jobject) {
    jfloatArray jArr = env->NewFloatArray(costs.size());
    env->SetFloatArrayRegion(jArr, 0, costs.size(), &costs[0]);
    costs.empty();

    return jArr;
}

JNIEXPORT jintArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionDemands
        (JNIEnv *env, jobject) {
    jintArray jArr = env->NewIntArray(demands.size());
    env->SetIntArrayRegion(jArr, 0, demands.size(), &demands[0]);
    demands.empty();

    return jArr;
}

JNIEXPORT jobjectArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionCustomers
        (JNIEnv *env, jobject) {
    int yDim = customers.size();
    int xDim = 0;
    for_each(customers.begin(), customers.end(), [&](vector<int> c) {
        if (c.size() > xDim) xDim = c.size();
    });

    jintArray inner = env->NewIntArray(xDim);
    jobjectArray outer = env->NewObjectArray(yDim, env->GetObjectClass(inner), 0);

    for (std::size_t i = 0; i < yDim; ++i) {
        env->SetIntArrayRegion(inner, 0, customers.at(i).size(), &customers.at(i)[0]);
        env->SetObjectArrayElement(outer, i, inner);

        if (i + 1 != yDim) {
            inner = env->NewIntArray(xDim);
        }
    }

    customers.empty();

    return outer;
}

JNIEXPORT jint JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_solve
        (JNIEnv *env, jobject) {
    problem->setInstCode("java");
    problem->setInstance("java");
    problem->setDepots(vSize);
    problem->setVehicles(vSize);
    problem->setCustomers(cSize);

    problem->allocateMemory();
    problem->setBestKnowSolution(1.0);

    for (int i = 0; i < problem->getDepots(); ++i) {
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
    if (explicitC2CDistance) {
        problem->setCustomerDistances(customerDistances);

        double totalC2CDistance = 0.0;
        for (int i = 0; i < problem->getCustomers(); ++i) {
            for (int j = 0; j < problem->getCustomers(); ++j) {
                if (i != j) {
                    totalC2CDistance += problem->getCustomerDistances().at(i).at(j);
                }
            }
        }
        problem->setAvgCustomerDistance(
                totalC2CDistance / (double) (problem->getCustomers() * problem->getCustomers()));
    }

    if (explicitD2CDistance) {
        problem->setDepotDistances(depotDistances);

        double totalD2CDistance = 0.0;
        for (int i = 0; i < problem->getDepots(); ++i) {
            for (int j = 0; j < problem->getCustomers(); ++j) {
                totalD2CDistance += problem->getDepotDistances().at(i).at(j);
            }
        }
        problem->setAvgDepotDistance(totalD2CDistance / (double) (problem->getCustomers() * 1.0));
    }

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

    printf("Instance....: %s\n", problem->getInstance().c_str());
    printf("Depots......: %d\n", problem->getDepots());
    printf("Vehicles....: %d\n", problem->getVehicles());
    printf("Capacity....: %d\n", problem->getCapacities().at(0));
    printf("Duration....: %.2f\n", problem->getDurations().at(0));
    printf("C2C Avg Dist: %.2f\n", problem->getAvgCustomerDistance());
    printf("D2C Avg Dist: %.2f\n", problem->getAvgDepotDistance());
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

    // Search solution
    try {
        community->manager();
    } catch (exception &e) {
        cout << e.what();
    }

    // Solutions
    int numRoutes = 0;
    IndividualsGroup &best = community->getEliteGroup()->getBest();
    best.printSolution();

    for_each(best.getIndividuals().begin(), best.getIndividuals().end(), [&numRoutes](Individual &individual) {
        numRoutes += individual.getNumVehicles();
    });

    depots = vector<int>(0);
    routes = vector<int>(0);
    costs = vector<float>(0);
    demands = vector<int>(0);
    customers = vector<vector<int>>(0);

    for_each(best.getIndividuals().begin(), best.getIndividuals().end(), [&](Individual &individual) {
        individual.updateRoutesID();
        for_each(individual.getRoutes().begin(), individual.getRoutes().end(), [&](Route &route) {
            depots.push_back(route.getDepot());
            routes.push_back(route.getId());
            costs.push_back(route.getTotalCost());
            demands.push_back(route.getDemand());

            vector<int> vcustomers;
            for_each(route.getTour().begin(), route.getTour().end(), [&vcustomers](int customer) {
                vcustomers.push_back(customer);
            });
            customers.push_back(vcustomers);
        });
    });

    delete community;

    return 0;
}