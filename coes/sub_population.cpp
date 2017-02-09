/*
 * File:   Subpopulation.cpp
 * Author: fernando
 *
 * Created on July 22, 2014, 4:38 PM
 */

#include "sub_population.hpp"

/*
 * Constructors and Destructor
 */

Subpopulation::Subpopulation() {

}

Subpopulation::Subpopulation(MDVRPProblem *problem, AlgorithmConfig *config, EliteGroup *eliteGroup, int depot) :
        problem(problem), config(config), eliteGroup(eliteGroup), depot(depot) {
    this->createPairingStructure();
    this->setBest(Individual(this->getProblem(), this->getConfig(), this->getDepot(), -1));

}

Subpopulation::Subpopulation(const Subpopulation &other) :
        problem(other.problem), config(other.config), eliteGroup(other.eliteGroup),
        depot(other.depot), best(other.best), individuals(other.individuals),
        pairing(other.pairing) {

}

Subpopulation::~Subpopulation() {
    //delete[] locks;
}

/*
 * Getters and Setters
 */

Individual &Subpopulation::getBest() {
    return this->best;
}

void Subpopulation::setBest(Individual best) {
    this->best = best;
}

int Subpopulation::getDepot() const {
    return this->depot;
}

void Subpopulation::setDepot(int depot) {
    this->depot = depot;
}

//int Subpopulation::getId() const {
//    return this->id;
//}
//
//void Subpopulation::setId(int id) {
//    this->id = id;
//}

IndividualsGroup &Subpopulation::getIndividualsGroup() {
    return this->individuals;
}

void Subpopulation::setIndividualsGroup(IndividualsGroup individuals) {
    this->individuals = individuals;
}

vector<Pairing> &Subpopulation::getPairing() {
    return this->pairing;
}

void Subpopulation::setPairing(vector<Pairing> solutions) {
    this->pairing = solutions;
}

AlgorithmConfig *Subpopulation::getConfig() const {
    return this->config;
}

void Subpopulation::setConfig(AlgorithmConfig *config) {
    this->config = config;
}

MDVRPProblem *Subpopulation::getProblem() const {
    return this->problem;
}

void Subpopulation::setProblem(MDVRPProblem *problem) {
    this->problem = problem;
}

EliteGroup *Subpopulation::getEliteGroup() {
    return this->eliteGroup;
}

void Subpopulation::setEliteGroup(EliteGroup *eliteGroup) {
    this->eliteGroup = eliteGroup;
}

bool Subpopulation::isLocked() const {
    return locked;
}

void Subpopulation::setLocked(bool locked) {

    //    if (locked)
    //        this->getProblem()->getMonitor().getMutexLocker().lock();
    //    else
    //        this->getProblem()->getMonitor().getMutexLocker().unlock();

    this->locked = locked;
}

/*
 * Public Methods
 */

void Subpopulation::createIndividuals() {

    this->getIndividualsGroup().clear();

    this->getIndividualsGroup().setProblem(this->getProblem());
    this->getIndividualsGroup().setConfig(this->getConfig());
    this->getIndividualsGroup().setDepot(this->getDepot());

    //cout << "\n\nSubpop: " << this->getDepot() << endl;

    for (int ind = 0; ind < this->getConfig()->getNumSubIndDepots(); ++ind) {
        Individual individual = Individual(this->getProblem(), this->getConfig(), this->getDepot(), ind);
        individual.create();
        //individual.print(true);
        this->getIndividualsGroup().add(individual);
    }

    //this->createLockers();

}

void Subpopulation::createPairingStructure() {

    this->getPairing().clear();

    for (int id = 0; id < this->getConfig()->getNumSubIndDepots(); ++id) {
        Pairing pairing = Pairing(this->getProblem(), this->getConfig(), this->getDepot(), id);
        this->getPairing().push_back(pairing);
    }

}

void Subpopulation::pairingRandomly() {

    for_each(this->getPairing().begin(), this->getPairing().end(), [&](Pairing &pairing) {
        pairing.pairingRandomly();
    });

}

void Subpopulation::pairingAllVsBest() {

    for_each(this->getPairing().begin(), this->getPairing().end(), [&](Pairing &pairing) {
        pairing.pairingAllVsBest();
    });

}

void Subpopulation::printPairing() {

    cout << "\n\nSubpopulation::printPairing(): Dep: " << this->getDepot();
    cout << "\tBest: " << this->getBest().getTotalCost() << endl;

    for_each(this->getPairing().begin(), this->getPairing().end(), [&](Pairing &pairing) {
        pairing.print();
    });

}

void Subpopulation::evolve() {
    IndividualsGroup offsprings = IndividualsGroup(this->getProblem(), this->getConfig(), this->getDepot());

    if (this->getProblem()->getMonitor().isTerminated())
        return;

    for (auto ite = this->getIndividualsGroup().getIndividuals().begin(); ite != this->getIndividualsGroup().getIndividuals().end(); ++ite) {
        Individual ind = *ite;

        if (this->getProblem()->getMonitor().isTerminated())
            break;

        for (int l = 0; l < this->getConfig()->getNumOffspringsPerParent(); ++l) {
            Individual offspring = ind.evolve();

            if (offspring.getGene().size() != 0)
                offsprings.add(offspring);

            if (this->getProblem()->getMonitor().isTerminated())
                break;

        }
    };

    if (this->getProblem()->getMonitor().isTerminated())
        return;

    this->getIndividualsGroup().shrink(offsprings);

    for (auto ite = this->getIndividualsGroup().getIndividuals().begin();
         ite != this->getIndividualsGroup().getIndividuals().end(); ++ite) {

        if (this->getProblem()->getMonitor().isTerminated())
            break;

        Individual ind = *ite;
        ind.setLocked(false);
    }
}
