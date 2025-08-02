package net.derfruhling.cmake

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec

import javax.inject.Inject

abstract class CMakeConfigurationTarget implements Named {
    final String name
    final Property<OutputKind> outputKind
    final ListProperty<String> dependencies
    final ListProperty<String> buildArgs
    final Property<String> target
    final Property<Action<Exec>> buildTaskCreated
    final Property<String> artifactBaseName
    final Property<String> artifactSubDir

    private final CMakeConfiguration parent

    @Inject
    CMakeConfigurationTarget(String name, CMakeConfiguration parent, ObjectFactory objects) {
        this.name = name
        this.parent = parent

        this.outputKind = objects.property(OutputKind)
        this.dependencies = objects.listProperty(String)
        this.buildArgs = objects.listProperty(String)
        this.target = objects.property(String)
        this.buildTaskCreated = objects.property(Action<Exec>) as Property<Action<Exec>>
        this.artifactBaseName = objects.property(String)
        this.artifactSubDir = objects.property(String)
    }

    void outputKind(OutputKind value) {
        outputKind.set(value)
    }

    void outputKind(String value) {
        switch (value) {
            case 'static':
                outputKind OutputKind.STATIC_LIBRARY
                break
            case 'shared':
                outputKind OutputKind.SHARED_LIBRARY
                break
            default:
                throw new IllegalArgumentException("Bad output kind: $value")
        }
    }

    void buildArgs(String... args) {
        buildArgs.addAll(args)
    }

    void target(String target) {
        this.target.set(target)
    }

    void after(String name) {
        dependencies.add(name)
    }

    void outputSubDir(String directory) {
        artifactSubDir.set(directory)
    }
}
