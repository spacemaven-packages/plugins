package net.derfruhling.cmake

import net.derfruhling.gradle.NativeArtifactOutputKind
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.jetbrains.annotations.Nullable

import javax.inject.Inject

abstract class CMakeConfiguration implements Named {
    final String name
    final DirectoryProperty sourceDirectory
    final Property<NativeArtifactOutputKind> outputKind
    final MapProperty<String, String> definitions
    final ListProperty<String> dependencies, configureArgs, buildArgs
    final Property<Action<Exec>> configureTaskCreated, buildTaskCreated
    final NamedDomainObjectContainer<CMakeConfigurationTarget> targets

    @Inject
    CMakeConfiguration(String name, ObjectFactory objects, Project project) {
        this.name = name
        this.outputKind = objects.property(NativeArtifactOutputKind)
        this.definitions = objects.mapProperty(String, String)
        this.sourceDirectory = objects.directoryProperty()
        this.dependencies = objects.listProperty(String)
        this.configureArgs = objects.listProperty(String)
        this.buildArgs = objects.listProperty(String)
        this.configureTaskCreated = objects.property(Action<Exec>) as Property<Action<Exec>>
        this.buildTaskCreated = objects.property(Action<Exec>) as Property<Action<Exec>>
        this.targets = objects.domainObjectContainer(CMakeConfigurationTarget, newName -> {
            def value = objects.newInstance(CMakeConfigurationTarget, newName, this)
            value.target.convention(name)
            value.artifactBaseName.convention(value.target)
            value.artifactSubDir.convention(".")
            value.outputKind.convention(this.outputKind)
            return value
        })

        this.sourceDirectory.set(project.projectDir)

        buildArgs.addAll("--", "-j${Runtime.getRuntime().availableProcessors()}")

        define CMAKE_COLOR_DIAGNOSTICS: true,
               CMAKE_COLOR_MAKEFILE: true
    }

    void targets(Action<NamedDomainObjectContainer<CMakeConfigurationTarget>> action) {
        action.execute(this.targets)
    }

    void targets(@DelegatesTo(NamedDomainObjectContainer<CMakeConfigurationTarget>) Closure action) {
        action.setDelegate(this.targets)
        action.run()
    }

    void outputKind(NativeArtifactOutputKind value) {
        outputKind.set(value)
    }

    void outputKind(String value) {
        switch (value) {
            case 'static':
                outputKind NativeArtifactOutputKind.STATIC_LIBRARY
                break
            case 'shared':
                outputKind NativeArtifactOutputKind.SHARED_LIBRARY
                break
            default:
                throw new IllegalArgumentException("Bad output kind: $value")
        }
    }

    void configureArgs(String... args) {
        configureArgs.addAll(args)
    }

    void buildArgs(String... args) {
        buildArgs.addAll(args)
    }

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

    void after(String name) {
        dependencies.add(name)
    }
}