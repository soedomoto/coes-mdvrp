cmake_minimum_required(VERSION 2.8)
project(lib_mdvrp)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -pthread -fPIC")

set(HEADER_FILES include/graph.h
        include/individual.h
        include/operation.h
        include/operators.h
        include/population.h
        include/solver.h
        include/structs.h
        include/vertex.h)

# Build our library
set(SOURCE_FILES
        src/graph.cpp
        src/individual.cpp
        src/main.cpp
        src/operation.cpp
        src/operators.cpp
        src/population.cpp
        src/solver.cpp
        src/vertex.cpp)

add_library(lib_mdvrp ${SOURCE_FILES} ${HEADER_FILES})
target_include_directories(lib_mdvrp PRIVATE include)
add_executable(mdvrp_bin ${SOURCE_FILES} ${HEADER_FILES})
target_include_directories(mdvrp_bin PRIVATE include)

set_target_properties(lib_mdvrp PROPERTIES PREFIX "lib")
