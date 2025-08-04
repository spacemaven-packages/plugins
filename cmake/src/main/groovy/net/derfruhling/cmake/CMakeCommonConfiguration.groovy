package net.derfruhling.cmake

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.jetbrains.annotations.Nullable

abstract class CMakeCommonConfiguration {
    abstract MapProperty<String, String> getDefinitions()
    abstract ListProperty<String> getConfigureArgs()
    abstract ListProperty<String> getBuildArgs()

    CMakeCommonConfiguration() {}

    void define(Map<String, @Nullable Object> map) {
        map.forEach { key, value ->
            switch (value) {
                case null:
                    value = ""
                    break
                case true:
                    value = "ON"
                    break
                case false:
                    value = "OFF"
                    break
                default:
                    value = value.toString()
            }

            definitions.put(key, value)
        }
    }

    void configureArgs(String... args) {
        configureArgs.addAll(args)
    }

    void buildArgs(String... args) {
        buildArgs.addAll(args)
    }
}
