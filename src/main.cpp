/*  Universidade Federal do Paraná
    Trabalho de Graduação
    Orientador: Eduardo j. Spinosa
    Implementação:  Lucas Fernandes de Oliveira
                    Marcela Ribeiro de Oliveira
    (NÃO SEI MAIS O QUE TEM QUE POR AQUI (?))
*/

#include <iostream>
#include <fstream>
#include "optparse.h"
#include <graph.h>
#include <solver.h>
#include <operators.h>
#include <cstdlib>
#include <ctime>

int main(int argc, char const *const *argv){
    optparse::OptionParser parser = optparse::OptionParser();
    parser.add_option("-n", "--name").set_default("mdvrp").help("problem name. default: %default");
    parser.add_option("-d", "--debug").set_default(false).action("store_true").help(
            "enable debug process. default: %default");
    parser.add_option("-i", "--num_iteration").set_default(1000)
            .help("How many iterations the method should run. default: %default");
    parser.add_option("-j", "--num_iteration_migration").set_default(5)
            .help("How many iterations between migrations. default: %default");
    parser.add_option("-k", "--num_iteration_in_migration").set_default(1)
            .help("How many iterations to a individual migrate: %default");
    parser.add_option("-P", "--num_populations").set_default(1)
            .help("Number of populations for the same route: %default");
    parser.add_option("-p", "--population_size").set_default(10)
            .help("How many individuals there are in each population. default: %default");
    parser.add_option("-M", "--max_migration").set_default(1)
            .help("Perform maximum number of migrations per cicle, otherwise 1 migration per clicle. default: %default");
    parser.add_option("-m", "--mutation_ratio").set_default(0.2)
            .help("Mutation Ratio [0.000 - 1.000]. default: %default");
    parser.add_option("-s", "--seed").set_default(0)
            .help("The seed to start random numbers in the method. default: %default");

    parser.usage(parser.usage() + " <problem_file> <solution_file>");

    const optparse::Values &options = parser.parse_args(argc, argv);
    const std::vector<std::string> args = parser.args();

    if (args.size() != 2) {
        parser.print_help();
        std::cout.flush();
        return EXIT_FAILURE;
    }

    std::string fileName = args[0];
    std::string outFileName = args[1];

    int nIterations = options.get("num_iteration");
    int populationSize = options.get("population_size");
    float mutationRatio = options.get("mutation_ratio");
    int redundancy = options.get("num_populations");
    int itToMigrate = options.get("num_iteration_migration");
    int itToInnerMig = options.get("num_iteration_in_migration");
    bool maxMigrations = (atoi(options.get("max_migration")) == 0) ? true : false;
    int seed = options.get("seed");

    std::fstream input, output;
    int nVehicles, nCustomers, nDepots;
    double maxRouteDuration, capacity;

//        std::cout << "[ERROR]: Invalid parameters: the correct parameters are, in this order:" << std::endl;
//        std::cout << "<infile>        : File with instance parameters" << std::endl;
//        std::cout << "<it>            : How many iterations the method should run" << std::endl;
//        std::cout << "<pop size>      : How many individuals there arein each population" << std::endl;
//        std::cout << "<mut ratio>     : Mutation Ratio [0.000 - 1.000]" << std::endl;
//        std::cout << "<n_populations> : Number of populations for the same route" << std::endl;
//        std::cout << "<it-mig>        : How many iterations between migrations" << std::endl;
//        std::cout << "<it-in-mig>     : How many iterations to a individual migrate" << std::endl;
//        std::cout << "<migration mode>: 0 to perform maximum number of migrations per cicle, otherwise 1 migration per clicle" << std::endl;
//        std::cout << "<outfile>       : File with instance parameters" << std::endl;
//        std::cout << "[<seed>]        : The seed to start random numbers in the method" << std::endl;

    input.open(fileName, std::ifstream::in);
    if(!input.good()) {
        std::cout << "[ERROR]: Could not open file: did you put th path correctly?" << std::endl;
        return 0;
    }
    input >> nVehicles;
    input >> nCustomers;
    input >> nDepots;
    input >> maxRouteDuration;
    input >> capacity;
    Graph g(nCustomers, nDepots, nVehicles);
    for(int i=0; i<nCustomers; ++i){
        int id, duration, demand;
        double x, y;
        input >> id >> x >> y >> duration >> demand;
        if(!g.addVertex(id, duration, demand, x, y, CUSTOMER)){
            printf("\nO vertice %d ja foi inserido", id);
            return(0);
        }
    }
    for(int i=0; i<nDepots; ++i){
        int id, duration, demand;
        double x, y;
        input >> id >> x >> y >> duration >> demand;
        g.addVertex(id, duration, demand, x, y, DEPOT);
    }
    g.buildEdges();
    MutSwap mutOp(mutationRatio);
    CrCut crOp(nCustomers + nDepots);
    SelTour selOp(3);

    Operation op(mutOp, crOp, selOp);
    MDVRPSolver solver(op, g, maxRouteDuration, capacity, populationSize);
    const char *outStr = solver.solve(nIterations, itToMigrate, redundancy, itToInnerMig, maxMigrations, seed);

    output.open(outFileName, std::fstream::out);
    output << outStr;
    output.close();

    return 0;
}

