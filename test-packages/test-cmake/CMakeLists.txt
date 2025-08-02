cmake_minimum_required(VERSION 3.20)

project(the-test C)

if(NOT DEFINED LIBRARY_KIND)
    set(LIBRARY_KIND STATIC)
endif()

set(SOURCES test.c)

add_library(the-test ${LIBRARY_KIND} ${SOURCES})
