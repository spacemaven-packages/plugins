package net.derfruhling.cmake

import net.derfruhling.gradle.NativeTarget
import net.derfruhling.gradle.NativeArtifactOutputKind
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

import javax.inject.Inject

abstract class CMakeExtension {
    @Nested abstract CMakeTargetsDefinition getTargets()
    @Nested abstract CMakeVariantsDefinition getVariants()
    abstract Property<Task> getPublicHeadersArchive()
    final NamedDomainObjectContainer<CMakeConfiguration> configurations

    @Inject
    CMakeExtension(ObjectFactory objects) {
        configurations = objects.domainObjectContainer(
                CMakeConfiguration,
                name -> objects.newInstance(CMakeConfiguration, name)
        )
    }

    abstract RegularFileProperty getBuildFile()

    void cmake(File file) {
        getBuildFile().set(file)
    }

    void targets(Action<CMakeTargetsDefinition> action) {
        targets.targets.set([])
        action.execute(targets)
    }

    void variants(Action<CMakeVariantsDefinition> action) {
        variants.variants.set(Map.of())
        action.execute(variants)
    }

    void configurations(Action<NamedDomainObjectContainer<CMakeConfiguration>> c) {
        c.execute(configurations)
    }

    static Map<String, String> getArtifactName(String name, NativeArtifactOutputKind kind, NativeTarget target = NativeTarget.getCurrent()) {
        switch(target) {
            case NativeTarget.LINUX_X64:
                switch(kind) {
                    case NativeArtifactOutputKind.STATIC_LIBRARY:
                        return [link: "lib${name}.a"]
                    case NativeArtifactOutputKind.SHARED_LIBRARY:
                        return [link: "lib${name}.so", runtime: "lib${name}.so"]
                }
                break
            case NativeTarget.MACOS_X64:
            case NativeTarget.MACOS_AARCH64:
                switch(kind) {
                    case NativeArtifactOutputKind.STATIC_LIBRARY:
                        return [link: "lib${name}.a"]
                    case NativeArtifactOutputKind.SHARED_LIBRARY:
                        return [link: "lib${name}.dylib", runtime: "lib${name}.dylib"]
                }
                break
            case NativeTarget.WINDOWS_X64:
                switch(kind) {
                    case NativeArtifactOutputKind.STATIC_LIBRARY:
                        return [link: "${name}.lib"]
                    case NativeArtifactOutputKind.SHARED_LIBRARY:
                        return [link: "${name}.lib", runtime: "${name}.dll"]
                }
                break
        }
    }

    @FunctionalInterface
    interface VariantAcceptor {
        void execute(String name, boolean isDebuggable, boolean isOptimized, NativeTarget target)
    }

    void allVariants(Action<String> action) {
        for(final def variant in variants.variants.get()) {
            for(final def target in targets.targets.get()) {
                action.execute("${variant}${target.variantName.capitalize()}")
            }
        }
    }
}
