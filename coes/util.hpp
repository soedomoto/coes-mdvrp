/*
 * File:   Util.hpp
 * Author: Fernando B Oliveira <fboliveira25@gmail.com>
 *
 * Created on July 23, 2014, 12:21 AM
 */

#ifndef UTIL_H
#define    UTIL_H

#include <algorithm>
#include <cfloat>
#include <cstring>
#include <vector>
#include <algorithm>
#include <cmath>
#include <thread>
#include <iostream>

#include "global.hpp"

using namespace std;

class Util {

public:
    Util();

    template<class Iter>
    static int findValueInVector(Iter begin, Iter end, float value);

    template<typename Iter>
    static int countElementsInVector(Iter begin, Iter end, int value);

    static bool isBetterSolution(float newCost, float currentCost);

    static bool isBetterOrEqualSolution(float newCost, float currentCost);

    static bool isEqualSolution(float newCost, float currentCost);

    static float scaledFloat(float value);

    static float calculateGAP(float cost, float bestKnowSolution);

    static double diffTimeFromStart(time_t start);

    static void printTimeNow();

    static void error(const string message, int num);

    static void print(vector<int> &vector);

    template<typename Iter>
    static void print(Iter begin, Iter end);

    static void selectVectorOrder(vector<typedef_order> &order);

    static float calculateEucDist2D(int x1, int y1, int x2, int y2);

private:

};

class Random {

public:

    static void randomize();

    static double randDoubleBetween(double x0, double x1);

    static float randFloat();

    static int randInt();

    static int randIntBetween(int x0, int x1);

    static void randPermutation(int size, int min, vector<int> &elements);

    static void randTwoNumbers(int min, int max, int &n1, int &n2);

    static int discreteDistribution(int min, int max);

private:

    Random();

    static int uniformIntDiscreteDistribution(int min, int max);

    static int binomialDistribution(int max, float p);

};

#endif	/* UTIL_H */

