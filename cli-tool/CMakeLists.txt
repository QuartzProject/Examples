cmake_minimum_required(VERSION 3.28)
project(qlog CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Sibling clones in the QuartzProject layout by default; pass -DQUARTZ_SOURCE_DIR and -DQUARTZ_EMBED_SOURCE_DIR to
# build against other checkouts.
set(QUARTZ_SOURCE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../Quartz" CACHE PATH "A Quartz checkout")
set(QUARTZ_EMBED_SOURCE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../Embed" CACHE PATH "A Quartz Embed checkout")
list(APPEND CMAKE_MODULE_PATH "${QUARTZ_EMBED_SOURCE_DIR}/cmake")
include(QuartzEmbed)

# qlog may read the log files its user names, wherever they are, and nothing else outside the working directory.
quartz_add_executable(qlog SOURCES src/main.qz src/formats.qz src/report.qz DENY_WARNINGS GRANT_ARGUMENT_PATHS)
