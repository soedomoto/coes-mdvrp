/*
 * File:   Config.cpp
 * Author: fernando
 *
 * Created on July 21, 2014, 4:22 PM
 */

#include "algorithm_config.hpp"

/*
 * Constructors
 */

AlgorithmConfig::AlgorithmConfig() {

}

/*
 * Getters and Setters
 */

Enum_Algorithms AlgorithmConfig::getAlgorithm() const {
    return algorithm;
}

void AlgorithmConfig::setAlgorithm(Enum_Algorithms algorithm) {
    this->algorithm = algorithm;
}

float AlgorithmConfig::getCapacityPenalty() const {
    return capacityPenalty;
}

void AlgorithmConfig::setCapacityPenalty(float capacityPenalty) {
    this->capacityPenalty = capacityPenalty;
}

bool AlgorithmConfig::isDebug() const {
    return debug;
}

void AlgorithmConfig::setDebug(bool debug) {
    this->debug = debug;
}

bool AlgorithmConfig::isDisplay() const {
    return display;
}

void AlgorithmConfig::setDisplay(bool display) {
    this->display = display;
}

float AlgorithmConfig::getExecutionTime() const {
    return executionTime;
}

void AlgorithmConfig::setExecutionTime(float executionTime) {
    this->executionTime = executionTime;
}

float AlgorithmConfig::getMaxTimeWithoutUpdate() const {
    return maxTimeWithoutUpdate;
}

void AlgorithmConfig::setMaxTimeWithoutUpdate(float maxTimeWithoutUpdate) {
    this->maxTimeWithoutUpdate = maxTimeWithoutUpdate;
}

float AlgorithmConfig::getExtraVehiclesPenalty() const {
    return extraVehiclesPenalty;
}

void AlgorithmConfig::setExtraVehiclesPenalty(float extraVehiclesPenalty) {
    this->extraVehiclesPenalty = extraVehiclesPenalty;
}

float AlgorithmConfig::getIncompleteSolutionPenalty() const {
    return incompleteSolutionPenalty;
}

void AlgorithmConfig::setIncompleteSolutionPenalty(float incompleteSolutionPenalty) {
    this->incompleteSolutionPenalty = incompleteSolutionPenalty;
}

Enum_Local_Search_Type AlgorithmConfig::getLocalSearchType() const {
    return localSearchType;
}

void AlgorithmConfig::setLocalSearchType(Enum_Local_Search_Type localSearchType) {
    this->localSearchType = localSearchType;
}

float AlgorithmConfig::getMutationRatePLS() const {
    return mutationRatePLS;
}

void AlgorithmConfig::setMutationRatePLS(float mutationRatePLS) {
    this->mutationRatePLS = mutationRatePLS;
}

float AlgorithmConfig::getMutationRatePM() const {
    return mutationRatePM;
}

void AlgorithmConfig::setMutationRatePM(float mutationRatePM) {
    this->mutationRatePM = mutationRatePM;
}

unsigned long int AlgorithmConfig::getNumGen() const {
    return numGen;
}

void AlgorithmConfig::setNumGen(unsigned long int numGen) {
    this->numGen = numGen;
}

int AlgorithmConfig::getNumSolutionElem() const {
    return numSolutionElem;
}

void AlgorithmConfig::setNumSolutionElem(int numSolutionElem) {
    this->numSolutionElem = numSolutionElem;
}

int AlgorithmConfig::getNumSubIndDepots() const {
    return numSubIndDepots;
}

void AlgorithmConfig::setNumSubIndDepots(int numSubIndDepots) {
    this->numSubIndDepots = numSubIndDepots;
}

int AlgorithmConfig::getNumOffspringsPerParent() const {
    return numOffspringsPerParent;
}

void AlgorithmConfig::setNumOffspringsPerParent(int numOffspringsPerParent) {
    this->numOffspringsPerParent = numOffspringsPerParent;
}

Enum_Process_Type AlgorithmConfig::getProcessType() const {
    return processType;
}

void AlgorithmConfig::setProcessType(Enum_Process_Type processType) {
    this->processType = processType;
}

float AlgorithmConfig::getRouteDurationPenalty() const {
    return routeDurationPenalty;
}

void AlgorithmConfig::setRouteDurationPenalty(float routeDurationPenalty) {
    this->routeDurationPenalty = routeDurationPenalty;
}

const char *AlgorithmConfig::getSaveLogRunFile() const {
    return saveLogRunFile;
}

void AlgorithmConfig::setSaveLogRunFile(const char *saveLogRunFile) {
    this->saveLogRunFile = saveLogRunFile;
}

Enum_StopCriteria AlgorithmConfig::getStopCriteria() const {
    return stopCriteria;
}

void AlgorithmConfig::setStopCriteria(Enum_StopCriteria stopCriteria) {
    this->stopCriteria = stopCriteria;
}

int AlgorithmConfig::getTotalMoves() const {
    return totalMoves;
}

void AlgorithmConfig::setTotalMoves(int totalMoves) {
    this->totalMoves = totalMoves;
}

bool AlgorithmConfig::isWriteFactors() const {
    return writeFactors;
}

void AlgorithmConfig::setWriteFactors(bool writeFactors) {
    this->writeFactors = writeFactors;
}

int AlgorithmConfig::getEliteGroupLimit() const {
    return this->eliteGroupLimit;
}

void AlgorithmConfig::setEliteGroupLimit(int eliteGroupLimit) {
    this->eliteGroupLimit = eliteGroupLimit;
}

/*
 * Methods
 */

void AlgorithmConfig::setParameters(MDVRPProblem *problem) {

    // Algorithm to be executed
    // this->setAlgorithm(Enum_Algorithms::ES);

    // Stop criterion used
    // this->setStopCriteria(TEMPO);
    //this->setStopCriteria(NUM_GER);

    // Type of processing
    // this->setProcessType(MULTI_THREAD);

    // Display or not information during the process
    // this->setDisplay(true);

    // Execution timeout (s) for criterion Stop == TIME
    // this->setExecutionTime(10 * 60.0); // 1.800 seg

    // Max time without update
    // this->setMaxTimeWithoutUpdate(5 * 60.0); // 600 seg.

    // Number of individuals in the subpopulation of depots
    // Mu value
    // this->setNumSubIndDepots(3);
    // Lambda value
    // this->setNumOffspringsPerParent(1);

    // Elite group size
    // this->setEliteGroupLimit(5);

    // Number of elements in the solution subpopulation
    // n - Problem depositos
    // 1 - FO
    this->setNumSolutionElem(problem->getDepots() + 1);

    // PARAMETERS FOR EVOLUTION -------
    // - The list of de Mutation
    // -- Random
    // this->setMutationRatePM(1.0);
    // -- Local Search
    // this->setLocalSearchType(Enum_Local_Search_Type::RANDOM);
    // this->setMutationRatePLS(0.2);
    // ----------------------------

    // Penalty for restricting the capacity of vehicles
    // this->setCapacityPenalty(pow(10, 3));

    // Extra vehicles
    // -- Penalty
    // this->setExtraVehiclesPenalty(pow(10, 3));
    // -- Relaxed version
    //this->setExtraVehiclesPenalty(0.0);

    // Route duration
//    if (problem->getDuration() > 0)
//        this->setRouteDurationPenalty(pow(10, 3));
//    else
//        this->setRouteDurationPenalty(0.0);

    // Penalty for incomplete solutions
    // this->setIncompleteSolutionPenalty(pow(10, 5));

    // Total movements - LS
    // this->setTotalMoves(9);

    // this->setWriteFactors(false);

}

string AlgorithmConfig::getLocalSearchTypeStringValue() {

    string ls;

    switch (this->getLocalSearchType()) {

        case Enum_Local_Search_Type::RANDOM:
            ls = "RND";
            break;

        case Enum_Local_Search_Type::SEQUENTIAL:
            ls = "SQT";
            break;

        case Enum_Local_Search_Type::NOT_APPLIED:
            ls = "NTA";
            break;

    }

    return ls;

}

bool AlgorithmConfig::isChange() const {
    return change;
}

void AlgorithmConfig::setChange(bool change) {
    AlgorithmConfig::change = change;
}
