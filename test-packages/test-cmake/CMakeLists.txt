cmake_minimum_required(VERSION 3.20)

project(the-test C)

if(NOT DEFINED LIBRARY_KIND)
    set(LIBRARY_KIND STATIC)
endif()

if(NOT DEFINED PLATFORM)
    message(FATAL_ERROR "PLATFORM is not defined!")
else()
    message(STATUS "Hello from ${PLATFORM}!")
endif()

set(SOURCES test.c)

add_library(the-test ${LIBRARY_KIND} ${SOURCES})
