/*
 * File:   Config.hpp
 * Author: fernando
 *
 * Created on July 21, 2014, 4:22 PM
 */

#ifndef CONFIG_HPP
#define    CONFIG_HPP

#include <cmath>
#include <climits>
#include <string>
#include <mutex>

#include "mdvrp_problem.hpp"
#include "global.hpp"

using namespace std;

class AlgorithmConfig {

private:

    // Algoritmo a ser executado
    Enum_Algorithms algorithm = Enum_Algorithms::ES;

    enum Enum_StopCriteria stopCriteria = TEMPO; // Criterio de parada utilizado
    enum Enum_Process_Type processType = MONO_THREAD; // Tipo de processamento

    bool change;
    bool debug = false;
    bool display = true;

    unsigned long int numGen;
    float executionTime = 10 * 60.0; // Tempo limite de execucao (s) para criterioParada == TEMPO
    float maxTimeWithoutUpdate = 5 * 60.0;

    int totalMoves = 9;
    const char *saveLogRunFile;

    int numSubIndDepots = 3; // Mu value
    int numOffspringsPerParent = 1; // Lambda value
    int numSolutionElem;

    float mutationRatePM = 1.0;

    enum Enum_Local_Search_Type localSearchType = Enum_Local_Search_Type::RANDOM;
    float mutationRatePLS = 0.2;

    float capacityPenalty = pow(10, 3);
    float routeDurationPenalty = pow(10, 3);
    float extraVehiclesPenalty = pow(10, 3);
    float incompleteSolutionPenalty = pow(10, 5);

    bool writeFactors = false;

    int eliteGroupLimit = 5;

public:

    AlgorithmConfig();

    Enum_Algorithms getAlgorithm() const;

    void setAlgorithm(Enum_Algorithms algorithm);

    float getCapacityPenalty() const;

    void setCapacityPenalty(float capacityPenalty);

    bool isChange() const;

    void setChange(bool change);

    bool isDebug() const;

    void setDebug(bool debug);

    bool isDisplay() const;

    void setDisplay(bool display);

    float getExecutionTime() const;

    void setExecutionTime(float executionTime);

    float getMaxTimeWithoutUpdate() const;

    void setMaxTimeWithoutUpdate(float maxTimeWithoutUpdate);

    float getExtraVehiclesPenalty() const;

    void setExtraVehiclesPenalty(float extraVehiclesPenalty);

    float getIncompleteSolutionPenalty() const;

    void setIncompleteSolutionPenalty(float incompleteSolutionPenalty);

    Enum_Local_Search_Type getLocalSearchType() const;

    void setLocalSearchType(Enum_Local_Search_Type localSearchType);

    float getMutationRatePLS() const;

    void setMutationRatePLS(float mutationRatePLS);

    float getMutationRatePM() const;

    void setMutationRatePM(float mutationRatePM);

    unsigned long int getNumGen() const;

    void setNumGen(unsigned long int numGen);

    int getNumSolutionElem() const;

    void setNumSolutionElem(int numSolutionElem);

    int getNumSubIndDepots() const;

    void setNumSubIndDepots(int numSubIndDepots);

    int getNumOffspringsPerParent() const;

    void setNumOffspringsPerParent(int numOffspringsPerParent);

    Enum_Process_Type getProcessType() const;

    void setProcessType(Enum_Process_Type processType);

    float getRouteDurationPenalty() const;

    void setRouteDurationPenalty(float routeDurationPenalty);

    const char *getSaveLogRunFile() const;

    void setSaveLogRunFile(const char *saveLogRunFile);

    Enum_StopCriteria getStopCriteria() const;

    void setStopCriteria(Enum_StopCriteria stopCriteria);

    int getTotalMoves() const;

    void setTotalMoves(int totalMoves);

    bool isWriteFactors() const;

    void setWriteFactors(bool writeFactors);

    int getEliteGroupLimit() const;

    void setEliteGroupLimit(int eliteGroupLimit);

    void setParameters(MDVRPProblem *problem);

    string getLocalSearchTypeStringValue();

    int getMaxDepot() const;

    void setMaxDepot(int maxDepot);

};

#endif	/* CONFIG_HPP */
