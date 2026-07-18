cmake_minimum_required(VERSION 3.28)
project(quartz_arena CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# The Quartz core, built from a checkout; the default is the sibling clone in the QuartzProject layout.
set(QUARTZ_SOURCE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../Quartz" CACHE PATH "A Quartz checkout")
if(NOT EXISTS "${QUARTZ_SOURCE_DIR}/include/quartz/Quartz.hpp")
    message(FATAL_ERROR "no Quartz checkout at ${QUARTZ_SOURCE_DIR}; pass -DQUARTZ_SOURCE_DIR=<Quartz>")
endif()
add_subdirectory("${QUARTZ_SOURCE_DIR}" "${CMAKE_BINARY_DIR}/quartz" EXCLUDE_FROM_ALL SYSTEM)

add_executable(arena src/Main.cpp src/World.cpp src/Script.cpp)
target_link_libraries(arena PRIVATE quartz)
