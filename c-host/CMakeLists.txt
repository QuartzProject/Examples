cmake_minimum_required(VERSION 3.28)
project(quartz_fleet C CXX)

set(CMAKE_C_STANDARD 17)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 23)

# The Quartz core, built from a checkout; the default is the sibling clone in the QuartzProject layout.
set(QUARTZ_SOURCE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../Quartz" CACHE PATH "A Quartz checkout")
if(NOT EXISTS "${QUARTZ_SOURCE_DIR}/include/quartz/quartz.h")
    message(FATAL_ERROR "no Quartz checkout at ${QUARTZ_SOURCE_DIR}; pass -DQUARTZ_SOURCE_DIR=<Quartz>")
endif()
add_subdirectory("${QUARTZ_SOURCE_DIR}" "${CMAKE_BINARY_DIR}/quartz" EXCLUDE_FROM_ALL SYSTEM)

add_executable(fleet fleet.c)
target_compile_options(fleet PRIVATE -Wall -Wextra -Wpedantic)
# quartz_c is the C interface over a C++ library, so the C++ runtime has to be linked in.
target_link_libraries(fleet PRIVATE quartz_c)
set_target_properties(fleet PROPERTIES LINKER_LANGUAGE CXX)
