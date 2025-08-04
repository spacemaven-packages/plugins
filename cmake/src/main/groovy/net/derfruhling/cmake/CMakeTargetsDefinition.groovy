package net.derfruhling.cmake

import net.derfruhling.gradle.NativeTarget
import org.gradle.api.provider.ListProperty

abstract class CMakeTargetsDefinition {
    abstract ListProperty<NativeTarget> getTargets()

    void linuxX64() { targets.add(NativeTarget.LINUX_X64) }
    void macosX64() { targets.add(NativeTarget.MACOS_X64) }
    void macosArm64() { targets.add(NativeTarget.MACOS_AARCH64) }
    void windowsX64() { targets.add(NativeTarget.WINDOWS_X64) }
    void all() { targets.addAll(NativeTarget.values()) }
}
