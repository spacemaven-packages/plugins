package net.derfruhling.cmake

import org.gradle.api.provider.ListProperty

abstract class CMakeTargetsDefinition {
    abstract ListProperty<CMakeTarget> getTargets()

    void linuxX64() { targets.add(CMakeTarget.LINUX_X64) }
    void macosX64() { targets.add(CMakeTarget.MACOS_X64) }
    void macosArm64() { targets.add(CMakeTarget.MACOS_AARCH64) }
    void windowsX64() { targets.add(CMakeTarget.WINDOWS_X64) }
    void all() { targets.addAll(CMakeTarget.values()) }
}
