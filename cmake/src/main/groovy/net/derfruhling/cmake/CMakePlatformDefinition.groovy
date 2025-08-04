package net.derfruhling.cmake

import net.derfruhling.gradle.NativeTarget

class CMakePlatformDefinition {
    final NativeTarget target
    final boolean aggregate
    final CMakeConfigurationPlatform platformConfig

    CMakePlatformDefinition(NativeTarget target, boolean aggregate, CMakeConfigurationPlatform platformConfig) {
        this.target = target
        this.aggregate = aggregate
        this.platformConfig = platformConfig
    }
}
