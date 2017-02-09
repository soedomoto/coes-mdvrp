cmake_minimum_required(VERSION 2.8)
project(lib_jcoes)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -pthread -fPIC")

# Find necessary packages
#set(LIB_COES_INCLUDE_DIRS "/media/soedomoto/DATA/ITB2015/EL5090 - Research Method/Research/Dynamic Enumerator Allocation/App/lib_coes/coes")
#message("CoES includes: " ${LIB_COES_INCLUDE_DIRS})
#include_directories("/media/soedomoto/DATA/ITB2015/EL5090 - Research Method/Research/Dynamic Enumerator Allocation/App/lib_coes/coes")
#link_directories("/media/soedomoto/DATA/ITB2015/EL5090 - Research Method/Research/Dynamic Enumerator Allocation/App/lib_coes/cmake-build-debug/")

include_directories(coes)

find_package(JNI)
message("JNI includes: " ${JNI_INCLUDE_DIRS})
include_directories(${JNI_INCLUDE_DIRS})

# Build our library
set(SOURCE_FILES
        com_soedomoto_vrp_solver_CoESVRPJNI.h
        com_soedomoto_vrp_solver_CoESVRPJNI.cpp
        coes/algorithm_config.cpp
        coes/algorithm_config.hpp
        coes/community.cpp
        coes/community.hpp
        coes/customer_position.cpp
        coes/customer_position.hpp
        coes/elite_group.cpp
        coes/elite_group.hpp
        coes/mdvrp_es_coevol.cpp
        coes/mdvrp_es_coevol.hpp
        coes/individual.cpp
        coes/individual.hpp
        coes/individuals_group.cpp
        coes/individuals_group.hpp
        coes/local_search.cpp
        coes/local_search.hpp
        coes/lock.cpp
        coes/lock.hpp
        coes/mdvrp_problem.cpp
        coes/mdvrp_problem.hpp
        coes/monitor.cpp
        coes/monitor.hpp
        coes/pairing.cpp
        coes/pairing.hpp
        coes/path_relinking.cpp
        coes/path_relinking.hpp
        coes/rank.cpp
        coes/rank.hpp
        coes/route.cpp
        coes/route.hpp
        coes/sub_population.cpp
        coes/sub_population.hpp
        coes/util.cpp
        coes/util.hpp
        coes/global.hpp
        )

add_library(lib_jcoes SHARED ${SOURCE_FILES})
# target_link_libraries(lib_jcoes ${JNI_LIBRARIES} liblib_coes.so)

# message("JNI Libraries: " ${JNI_LIBRARIES})
# target_link_libraries(lib_jcoes ${JNI_LIBRARIES})

# Define the wrapper library that wraps our library
# target_link_libraries(lib_jcoes ~/lib_coes.so)

# don't prepend wrapper library name with lib
set_target_properties(lib_jcoes PROPERTIES PREFIX "lib")
