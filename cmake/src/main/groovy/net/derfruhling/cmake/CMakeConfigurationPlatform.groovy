package net.derfruhling.cmake

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.jetbrains.annotations.Nullable

import javax.inject.Inject

abstract class CMakeConfigurationPlatform extends CMakeCommonConfiguration {
    final MapProperty<String, String> definitions
    final ListProperty<String> configureArgs
    final ListProperty<String> buildArgs
    final MapProperty<String, String> outputFileNames

    @Inject
    CMakeConfigurationPlatform(ObjectFactory objects) {
        definitions = objects.mapProperty(String, String)
        configureArgs = objects.listProperty(String)
        buildArgs = objects.listProperty(String)
        outputFileNames = objects.mapProperty(String, String)
    }

    void outputFile(Map<String, String> map) {
        outputFileNames.putAll(map)
    }
}
