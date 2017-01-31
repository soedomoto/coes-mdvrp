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

void Util::selectVectorOrder(vector<typedef_order> & order) {

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
