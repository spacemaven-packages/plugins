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
    final Property<String> outputFileName, extension

    @Inject
    CMakeConfigurationPlatform(ObjectFactory objects) {
        definitions = objects.mapProperty(String, String)
        configureArgs = objects.listProperty(String)
        buildArgs = objects.listProperty(String)
        outputFileName = objects.property(String)
        extension = objects.property(String)
    }
}
