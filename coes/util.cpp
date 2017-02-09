/*
 * File:   Util.cpp
 * Author: Fernando B Oliveira <fboliveira25@gmail.com>
 *
 * Created on July 23, 2014, 12:21 AM
 */

#include "util.hpp"
#include "algorithm_config.hpp"
#include "route.hpp"
#include "local_search.hpp"

/*
 * Constructors
 */

Util::Util() {
}

/*
 * Getters and Setters
 */



/*
 * Methods
 */

template<class Iter>
int Util::findValueInVector(Iter begin, Iter end, float value) {

    //
    // http://stackoverflow.com/questions/18138075/vector-iterator-parameter-as-template-c

    // http://stackoverflow.com/questions/571394/how-to-find-an-item-in-a-stdvector
    // find item
    // std::find(vector.begin(), vector.end(), item)!=vector.end()

    // http://stackoverflow.com/questions/15099707/how-to-get-position-of-a-certain-element-in-strings-vector-to-use-it-as-an-inde

    Iter it = find(begin, end, value);

    if (it == end)
        return -1;
    else
        return it - begin;

}

template<typename Iter>
int Util::countElementsInVector(Iter begin, Iter end, int value) {
    // http://www.cplusplus.com/reference/algorithm/count/
    return std::count(begin, end, value);
}

bool Util::isBetterSolution(float newCost, float currentCost) {

    if (scaledFloat(newCost) >= 0 &&
        scaledFloat(newCost) < scaledFloat(currentCost)) // || ((newCost - currentCost) < 0.001))
        return true;
    else
        return false;

}


bool Util::isBetterOrEqualSolution(float newCost, float currentCost) {

    if (scaledFloat(newCost) >= 0 &&
        scaledFloat(newCost) <= scaledFloat(currentCost)) // || ((newCost - currentCost) < 0.001))
        return true;
    else
        return false;

}

bool Util::isEqualSolution(float newCost, float currentCost) {
    if (scaledFloat(newCost) >= 0 &&
        scaledFloat(newCost) == scaledFloat(currentCost)) // || ((newCost - currentCost) < 0.001))
        return true;
    else
        return false;
}

float Util::scaledFloat(float value) {

    if (value == FLT_MAX)
        return value;

    long long int scaled = value * 100;
    value = static_cast<float>(scaled) / 100.0;
    return value;

}

float Util::calculateGAP(float cost, float bestKnowSolution) {
    return ((cost - bestKnowSolution) / bestKnowSolution) * 100.00;
}

double Util::diffTimeFromStart(time_t start) {

    time_t end;
    // double difftime (time_t end, time_t beginning);
    // Return difference between two times
    time(&end);

    return difftime(end, start);
}

void Util::printTimeNow() {

    const time_t ctt = time(0);
    cout << asctime(localtime(&ctt)) << "\n";

}

void Util::error(const string message, int num) {

    cout << "\n\nERRO: ==========================================\n";
    cout << "MSG: " << message << " - " << num << endl;
    cout << "================================================\n\n";

}

void Util::print(vector<int> &vector) {
    print(vector.begin(), vector.end());
}

template<typename Iter>
void Util::print(Iter begin, Iter end) {

    size_t size = end - begin;

    cout << "Size: " << size << " => ";

    for (Iter iter = begin; iter != end; ++iter) {
        cout << *iter;

        if (next(iter) != end)
            cout << " - ";
    }

    cout << endl;
}

void Util::selectVectorOrder(vector<typedef_order> &order) {

    size_t min, i, j;
    typedef_order temp;

    for (i = 0; i < order.size() - 1; ++i) {

        min = i;
        // Procura pelo menor elemento

        for (j = i + 1; j < order.size(); ++j) {

            if (order.at(j).cost < order.at(min).cost)
                min = j;
        }

        // Substitui o menor elemento com o elemento em i
        temp = order.at(min);
        order.at(min) = order.at(i);
        order.at(i) = temp;
    }
}

float Util::calculateEucDist2D(int x1, int y1, int x2, int y2) {

    int xd = x1 - x2;
    int yd = y1 - y2;

    return sqrtf(xd * xd + yd * yd);
}

/*
 * Constructors
 */

Random::Random() {
}

/*
 * Getters and Setters
 */

/*
 * Methods
 */

void Random::randomize() {
    srand(std::time(0));
}

double Random::randDoubleBetween(double x0, double x1) {
    // Define os numeros aleatorios conforme limite inferior e superior
    // limiteInferior + aleatorios * (limiteSuperior - limiteInferior);
    //return x0 + (x1 - x0) * randfloat(); // / ((double) RAND_MAX);

    //return (randfloat() % x1) + x0;
    Util::error("TO-DO: randdoublebet", 0);
    return -1.0;

}

float Random::randFloat() {
    return (float) rand() / (float) RAND_MAX;
}

int Random::randInt() {
    return randIntBetween(0, RAND_MAX);
}

// Numero inteiro aleatorio [x0..x1]

int Random::randIntBetween(int x0, int x1) {
    // Define os numeros aleatorios conforme limite inferior e superior
    // limiteInferior + aleatorios * (limiteSuperior - limiteInferior);

    // http://stackoverflow.com/questions/4413170/c-generating-a-truly-random-number-between-10-20

    if (x0 > x1) {
        Util::error("randIntBetween: x0 > x1 ", -1);
        cout << "x0: " << x0 << "\tx1: " << x1 << "\n";
        return -1;
    }

    return (std::rand() % (x1 + 1 - x0)) + x0;

//    return uniformIntDiscreteDistribution(x0, x1);

}

void Random::randPermutation(int size, int min, vector<int> &elements) {

    int i;

    // inizialize
    for (i = 0; i < size; ++i) {
        elements.push_back(min);
        min++;
    }

    random_shuffle(elements.begin(), elements.end());

}

void Random::randTwoNumbers(int min, int max, int &n1, int &n2) {

    int x = randIntBetween(min, max);
    int y = x;

    while (y == x) {
        y = randIntBetween(min, max);
    }

    if (x < y) {
        n1 = x;
        n2 = y;
    } else {
        n1 = y;
        n2 = x;
    }

}

int Random::discreteDistribution(int min, int max) {
    return binomialDistribution(max, 0.5) + min;
}

/*
 * Private Methods
 */

int Random::uniformIntDiscreteDistribution(int min, int max) {

    // Uniform discrete distribution
    // http://www.cplusplus.com/reference/random/uniform_int_distribution/

    // http://www.cplusplus.com/reference/random/uniform_int_distribution/operator()/
    unsigned seed = std::chrono::system_clock::now().time_since_epoch().count();
    std::default_random_engine generator(seed);

    std::uniform_int_distribution<int> distribution(min, max);

    return distribution(generator);

}

int Random::binomialDistribution(int max, float p) {

    //http://www.cplusplus.com/reference/random/binomial_distribution/operator()/

    // construct a trivial random generator engine from a time-based seed:
    unsigned seed = std::chrono::system_clock::now().time_since_epoch().count();
    std::default_random_engine generator(seed);

    std::binomial_distribution<int> distribution(max, p);
    return distribution(generator);

}
