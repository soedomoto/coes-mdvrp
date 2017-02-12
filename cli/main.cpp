#include <cstdlib>
#include <vector>
#include <iostream>
#include <mdvrp_es_coevol.hpp>
#include "optparse.h"

using namespace std;

void solve(MDVRPProblem *problem, AlgorithmConfig *config) {
    time_t start, end;

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

    ESCoevolMDVRP esCoevolMDVRP = ESCoevolMDVRP(problem, config);
    esCoevolMDVRP.run();

    time(&end);

    double dif = difftime(end, start);
    printf("\n\nProcesso finalizado. Tempo gasto = %lf\n\n", dif);
    printf("Encerramento: ");
    Util::printTimeNow();

    delete problem;
    delete config;
}

int main(int argc, char const *const *argv) {
    /**
     * Recomendation for production
     * this->setExecutionTime(30 * 60.0);
     * this->setMaxTimeWithoutUpdate(10 * 60.0);
     * this->setNumSubIndDepots(20);
     * this->setNumOffspringsPerParent(5);
     * this->setEliteGroupLimit(50);
     */

    optparse::OptionParser parser = optparse::OptionParser();
    parser.add_option("-n", "--name").set_default("mdvrp").help("problem name. default: %default");
    parser.add_option("-d", "--debug").set_default(false).action("store_true").help(
            "enable debug process. default: %default");
    parser.add_option("-i", "--depot-subpop-ind").set_default(3).help(
            "number of individuals in the subpopulation of depots. default: %default");
    parser.add_option("-t", "--max-exec-time").set_default(10 * 60.0).help("max execution time. default: %default");
    parser.add_option("-u", "--max-time-no-update").set_default(5 * 60.0).help(
            "max time without update. default: %default");
    parser.add_option("-m", "--mutation-rate").set_default(0.2).help("mutation rate. default: %default");
    parser.add_option("-o", "--offspring").set_default(1).help("offspring per parent. default: %default");
    parser.add_option("-e", "--elite-group").set_default(5).help("elite group limit. default: %default");
    parser.add_option("-w", "--write-factors").set_default(false).action("store_true").help("enable write factors");
    parser.add_option("-O", "--output").set_default("").help("enable save output");
    parser.add_option("-C", "--capacity-penalty").set_default(pow(10, 3)).help(
            "set capacity penalty. default: %default");
    parser.add_option("-V", "--vehicle-penalty").set_default(pow(10, 3)).help("set vehicle penalty. default: %default");
    parser.add_option("-D", "--duration-penalty").set_default(pow(10, 3)).help(
            "set duration penalty. default: %default");
    parser.add_option("-S", "--solution-penalty").set_default(pow(10, 5)).help(
            "set incomplete solution penalty. default: %default");
    parser.add_option("-M", "--total-moves").set_default(9).help("set total moves. default: %default");

    parser.usage(parser.usage() + " <problem_file> <solution_file>");

    const optparse::Values &options = parser.parse_args(argc, argv);
    const std::vector<std::string> args = parser.args();

    if (args.size() != 2) {
        parser.print_help();
        return EXIT_FAILURE;
    }

    bool change = false;
    int popSize;
    float executionTime;
    float maxTimeWithoutUpdate;
    float mutationRatePLS;
    int eliteGroupLimit;

    // Start solving
    MDVRPProblem *problem = new MDVRPProblem();
    AlgorithmConfig *config = new AlgorithmConfig();
    
    // Read data file
    if (!problem->processInstanceFiles(options["name"].c_str(), args[0].c_str(), args[1].c_str())) {
        return EXIT_FAILURE;
    }
    problem->printAllocationDependecy();

    config->setDebug(static_cast<bool>(options["debug"].c_str()));
    config->setNumSubIndDepots(atoi(options.get("depot_subpop_ind")));
    config->setExecutionTime(atof(options.get("max_exec_time")));
    config->setMaxTimeWithoutUpdate(atof(options["max_time_no_update"].c_str()));
    config->setMutationRatePLS(atof(options["mutation_rate"].c_str()));
    config->setNumOffspringsPerParent(atoi(options["offspring"].c_str()));
    config->setEliteGroupLimit(atoi(options["elite_group"].c_str()));
    config->setWriteFactors(static_cast<bool>(options["write_factors"].c_str()));
    if (options.get("output") != nullptr) {
        config->setSaveLogRunFile(options["output"].c_str());
    }
    config->setCapacityPenalty(atoi(options["capacity_penalty"].c_str()));
    config->setCapacityPenalty(atoi(options["vehicle_penalty"].c_str()));
    config->setRouteDurationPenalty(atoi(options["duration_penalty"].c_str()));
    config->setIncompleteSolutionPenalty(atoi(options["solution_penalty"].c_str()));
    config->setTotalMoves(atoi(options["total_moves"].c_str()));

    config->setProcessType(MONO_THREAD);

//    config->setAlgorithm(Enum_Algorithms::SSGPU);
//    config->setStopCriteria(TEMPO);
//    config->setProcessType(MONO_THREAD);
//    config->setLocalSearchType(Enum_Local_Search_Type::RANDOM);

    config->setParameters(problem);
    solve(problem, config);
}

