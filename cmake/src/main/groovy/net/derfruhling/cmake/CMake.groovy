package net.derfruhling.cmake

import net.derfruhling.gradle.NativeTarget
import org.gradle.api.attributes.Attribute

class CMake {
    static final CMAKE_TARGET_ATTRIBUTE = Attribute.of('net.derfruhling.cmake.target', NativeTarget)
}
