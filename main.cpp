#include <cstdlib>
#include <vector>
#include <iostream>
#include "coes/solver.h"
#include "coes/optparse.h"

using namespace std;

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
    Solver *solver = new Solver();
    // Read data file
    if (!solver->getProblem()->processInstanceFiles(options["name"].c_str(), args[0].c_str(), args[1].c_str())) {
        return EXIT_FAILURE;
    }
    solver->getProblem()->printAllocationDependecy();

    solver->getConfig()->setDebug(static_cast<bool>(options["debug"].c_str()));
    solver->getConfig()->setNumSubIndDepots(atoi(options.get("depot_subpop_ind")));
    solver->getConfig()->setExecutionTime(atof(options.get("max_exec_time")));
    solver->getConfig()->setMaxTimeWithoutUpdate(atof(options["max_time_no_update"].c_str()));
    solver->getConfig()->setMutationRatePLS(atof(options["mutation_rate"].c_str()));
    solver->getConfig()->setNumOffspringsPerParent(atoi(options["offspring"].c_str()));
    solver->getConfig()->setEliteGroupLimit(atoi(options["elite_group"].c_str()));
    solver->getConfig()->setWriteFactors(static_cast<bool>(options["write_factors"].c_str()));
    if (options.get("output") != nullptr) {
        solver->getConfig()->setSaveLogRunFile(options["output"].c_str());
    }
    solver->getConfig()->setCapacityPenalty(atoi(options["capacity_penalty"].c_str()));
    solver->getConfig()->setCapacityPenalty(atoi(options["vehicle_penalty"].c_str()));
    if (solver->getProblem()->getDuration() > 0) {
        solver->getConfig()->setRouteDurationPenalty(atoi(options["duration_penalty"].c_str()));
    }
    solver->getConfig()->setIncompleteSolutionPenalty(atoi(options["solution_penalty"].c_str()));
    solver->getConfig()->setTotalMoves(atoi(options["total_moves"].c_str()));

//    solver->getConfig()->setAlgorithm(Enum_Algorithms::SSGPU);
//    solver->getConfig()->setStopCriteria(TEMPO);
//    solver->getConfig()->setProcessType(MONO_THREAD);
//    solver->getConfig()->setLocalSearchType(Enum_Local_Search_Type::RANDOM);

    solver->solve();
}