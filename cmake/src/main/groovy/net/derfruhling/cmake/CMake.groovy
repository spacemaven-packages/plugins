package net.derfruhling.cmake

import org.gradle.api.attributes.Attribute

class CMake {
    static final CMAKE_TARGET_ATTRIBUTE = Attribute.of('net.derfruhling.cmake.target', CMakeTarget)
}
