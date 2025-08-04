package net.derfruhling.cmake

class CMakePlatformDefinition {
    final CMakeTarget target
    final boolean aggregate
    final CMakeConfigurationPlatform platformConfig

    CMakePlatformDefinition(CMakeTarget target, boolean aggregate, CMakeConfigurationPlatform platformConfig) {
        this.target = target
        this.aggregate = aggregate
        this.platformConfig = platformConfig
    }
}
