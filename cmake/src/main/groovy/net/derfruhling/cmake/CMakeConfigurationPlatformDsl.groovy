package net.derfruhling.cmake

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty

import java.util.function.Supplier

final class CMakeConfigurationPlatformDsl {
    final ListProperty<CMakePlatformDefinition> platforms

    private final ObjectFactory objects

    CMakeConfigurationPlatformDsl(ListProperty<CMakePlatformDefinition> platforms, ObjectFactory objects) {
        this.platforms = platforms
        this.objects = objects
    }

    private CMakeConfigurationPlatform getPlatform(CMakeTarget target) {
        def value = platforms.get().find { it.target == target && !it.aggregate }
        if(value != null) return value.platformConfig

        value = new CMakePlatformDefinition(target, false, objects.newInstance(CMakeConfigurationPlatform))
        platforms.add(value)
        return value.platformConfig
    }

    CMakeConfigurationPlatform getLinuxX64() { return getPlatform(CMakeTarget.LINUX_X64) }
    CMakeConfigurationPlatform getMacosX64() { return getPlatform(CMakeTarget.MACOS_X64) }
    CMakeConfigurationPlatform getMacosArm64() { return getPlatform(CMakeTarget.MACOS_AARCH64) }
    CMakeConfigurationPlatform getWindowsX64() { return getPlatform(CMakeTarget.WINDOWS_X64) }

    private CMakeConfigurationPlatform defineAggregatePlatform(CMakeTarget... targets) {
        assert targets.length > 0
        CMakePlatformDefinition value = null

        for(def target in targets) {
            def newValue = platforms.get().find { it.target == target && it.aggregate }
            if(newValue == null && value == null) {
                newValue = new CMakePlatformDefinition(target, true, objects.newInstance(CMakeConfigurationPlatform))
                platforms.add(newValue)
            } else if(newValue != value && value != null) {
                assert value != null
                newValue = new CMakePlatformDefinition(target, true, value.platformConfig)
                platforms.add(newValue)
            }

            if(value == null) value = newValue
        }

        return value.platformConfig
    }

    CMakeConfigurationPlatform getMacosAll() {
        return defineAggregatePlatform(CMakeTarget.MACOS_AARCH64, CMakeTarget.MACOS_X64)
    }
}
