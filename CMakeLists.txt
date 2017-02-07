cmake_minimum_required(VERSION 2.8)
project(lib_coes)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -pthread -fPIC")

# Build our library
set(SOURCE_FILES
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
        coes/random.cpp
        coes/random.hpp
        coes/rank.cpp
        coes/rank.hpp
        coes/route.cpp
        coes/route.hpp
        coes/sub_population.cpp
        coes/sub_population.hpp
        coes/util.cpp
        coes/util.hpp
        coes/global.hpp
        coes/solver.cpp
        coes/solver.h main.cpp coes/optparse.h)

# Define the wrapper library that wraps our library
add_library(lib_coes SHARED ${SOURCE_FILES})

# Add executable
add_executable(coes_bin ${SOURCE_FILES})

# don't prepend wrapper library name with lib
set_target_properties(lib_coes PROPERTIES PREFIX "lib")
