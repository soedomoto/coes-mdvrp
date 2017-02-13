#include <cstdlib>
#include <vector>
#include <iostream>
#include <individuals_group.hpp>
#include <elite_group.hpp>
#include <community.hpp>
#include <fstream>
#include "optparse.h"

using namespace std;

IndividualsGroup & solve(MDVRPProblem *problem, AlgorithmConfig *config) {
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

    // Instantiate variables
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

    time(&end);

    double dif = difftime(end, start);
    printf("\n\nProcesso finalizado. Tempo gasto = %lf\n\n", dif);
    printf("Encerramento: ");
    Util::printTimeNow();

    // Solutions
    int numRoutes = 0;
    IndividualsGroup &best = community->getEliteGroup()->getBest();
    best.printSolution();

    delete community;
    
    return best;
}

void saveOutput(const char *out, IndividualsGroup &best) {
    vector<string> depots = vector<string>(0);
    vector<string> routes = vector<string>(0);
    vector<string> costs = vector<string>(0);
    vector<string> demands = vector<string>(0);
    vector<vector<string>> customers = vector<vector<string>>(0);

    for_each(best.getIndividuals().begin(), best.getIndividuals().end(), [&depots, &routes, &costs, &demands, &customers](Individual &individual) {
        individual.updateRoutesID();
        for_each(individual.getRoutes().begin(), individual.getRoutes().end(), [&depots, &routes, &costs, &demands, &customers](Route &route) {
            depots.push_back(std::to_string(route.getDepot()));
            routes.push_back(std::to_string(route.getId()));
            costs.push_back(std::to_string(route.getTotalCost()));
            demands.push_back(std::to_string(route.getDemand()));

            vector<string> vcustomers;
            for_each(route.getTour().begin(), route.getTour().end(), [&vcustomers](int customer) {
                vcustomers.push_back(std::to_string(customer));
            });
            customers.push_back(vcustomers);
        });
    });

    vector<string> lines = vector<string>(0);
    ostringstream odepots;
    ostringstream oroutes;
    ostringstream ocosts;
    ostringstream odemands;
    ostringstream ocustomers;
    for(int i=0; i<depots.size(); i++) {
        odepots << depots.at(i);
        oroutes << routes.at(i);
        ocosts << costs.at(i);
        odemands << demands.at(i);

        ostringstream o;
        for(int c=0; c<customers.at(i).size(); c++) {
            o << customers.at(i).at(c);
            if(c != customers.at(i).size()-1) o << ",";
        }
        ocustomers << o.str();

        if(i != depots.size()-1) {
            odepots << "\t";
            oroutes << "\t";
            ocosts << "\t";
            odemands << "\t";
            ocustomers << "\t";
        }
    }

    lines.push_back(odepots.str());
    lines.push_back(oroutes.str());
    lines.push_back(ocosts.str());
    lines.push_back(odemands.str());
    lines.push_back(ocustomers.str());

    string strOut = string(out);
    ofstream outSolution(strOut, ios::out | ofstream::binary);
    ostream_iterator<string> outStream(outSolution, "\n");
    copy(lines.begin(), lines.end(), outStream);
    outSolution.flush();
    outSolution.close();
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

    if (args.size() != 3) {
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
    IndividualsGroup &best = solve(problem, config);

    saveOutput(args[2].c_str(), best);

    delete problem;
    delete config;
}

