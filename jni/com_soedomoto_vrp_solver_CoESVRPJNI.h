/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_soedomoto_vrp_solver_CoESVRPJNI */

static MDVRPProblem *problem = new MDVRPProblem();
static AlgorithmConfig *config = new AlgorithmConfig();

static jint vSize;
static jfloat *jvXDepots;
static jfloat *jvYDepots;
static jfloat *jvDurations;
static jint *jvCapacities;
static jint cSize;
static jfloat *jcXDepots;
static jfloat *jcYDepots;
static jint *jcDemands;

static bool explicitC2CDistance = false;
static typedef_vectorMatrix<float> customerDistances;
static bool explicitD2CDistance = false;
static typedef_vectorMatrix<float> depotDistances;

static vector<int> depots;
static vector<int> routes;
static vector<float> costs;
static vector<int> demands;
static vector<vector<int>> customers;

#ifndef _Included_com_soedomoto_vrp_solver_CoESVRPJNI
#define _Included_com_soedomoto_vrp_solver_CoESVRPJNI
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    setVehicles
 * Signature: (I[F[F[F[I)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setVehicles
  (JNIEnv *, jobject, jint, jfloatArray, jfloatArray, jfloatArray, jintArray);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    setCustomers
 * Signature: (I[F[F[I)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setCustomers
  (JNIEnv *, jobject, jint, jfloatArray, jfloatArray, jintArray);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    setCustomerToCustomerDistance
 * Signature: (II[[F)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setCustomerToCustomerDistance
        (JNIEnv *, jobject, jint, jint, jobjectArray);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    setDepotToCustomerDistance
 * Signature: (II[[F)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setDepotToCustomerDistance
        (JNIEnv *, jobject, jint, jint, jobjectArray);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    setDepotToDepotDistance
 * Signature: (II[[F)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_setDepotToDepotDistance
        (JNIEnv *, jobject, jint, jint, jobjectArray);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    configSetNumSubpopulation
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_configSetNumSubpopulation
        (JNIEnv *, jobject, jint);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    configStopWhenMaxExecutionTime
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_configStopWhenMaxExecutionTime
        (JNIEnv *, jobject, jfloat);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    configStopWhenMaxTimeWithoutUpdate
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_configStopWhenMaxTimeWithoutUpdate
        (JNIEnv *, jobject, jfloat);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    configStopWhenNumGeneration
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_configStopWhenNumGeneration
        (JNIEnv *, jobject, jint);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    solve
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_solve
  (JNIEnv *, jobject);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    getSolutionDepots
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionDepots
        (JNIEnv *, jobject);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    getSolutionRoutes
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionRoutes
        (JNIEnv *, jobject);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    getSolutionCosts
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionCosts
        (JNIEnv *, jobject);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    getSolutionDemands
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionDemands
        (JNIEnv *, jobject);

/*
 * Class:     com_soedomoto_vrp_solver_CoESVRPJNI
 * Method:    getSolutionCustomers
 * Signature: ()[[I
 */
JNIEXPORT jobjectArray JNICALL Java_com_soedomoto_vrp_solver_CoESVRPJNI_getSolutionCustomers
        (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
