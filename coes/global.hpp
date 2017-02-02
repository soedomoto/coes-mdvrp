/*
 * File:   global.h
 * Author: fernando
 *
 * Created on May 15, 2013, 9:30 PM
 */

#include <vector>
#include <list>
#include <iterator>
#include <string>
#include <time.h>

using namespace std;

#ifndef GLOBAL_H

#define    GLOBAL_H

// INSTANCE TEST
// #define INST_TEST "pr10"

// #define SOURCE 1
// #define DEBUG_VERSION true
// #define LOG_RUN_FILE "/home/soedomoto/eclipse/mdvrp-cuda/data/mdvrpcpu-20.txt"

#define NUM_MAX_DEP 9
#define NUM_MAX_CLI 360

enum class Enum_Algorithms {
    SSGA,
    SSGPU,
    ES
};

enum Enum_StopCriteria {
    NUM_GER,
    TEMPO
};

enum Enum_Process_Type {
    MONO_THREAD,
    MULTI_THREAD
};

enum Enum_Local_Search_Type {
    RANDOM,
    SEQUENTIAL,
    NOT_APPLIED
};

template<class T>
using typedef_vectorMatrix = vector<vector<T>>;

using typedef_vectorIntIterator = vector<int>::iterator;
using typedef_vectorIntSize = vector<int>::size_type;
using typedef_listIntIterator = list<int>::iterator;

typedef struct {
    int i; // id, indice, ...
    float x;
    float y;
} typedef_point;

typedef struct {
    int index;
    float cost;
} typedef_order;

typedef struct {
    double time;
    float cost;
} typedef_evolution;

typedef struct {
    double time;
    string text;
} typedef_log;

typedef struct {
    int depot;
    typedef_vectorIntIterator position;
    float cost;
} typedef_location;

#endif	/* GLOBAL_H */
