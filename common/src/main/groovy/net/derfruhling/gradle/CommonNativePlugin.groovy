package net.derfruhling.gradle


import net.derfruhling.spm.consumer.SpmExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask
import org.gradle.nativeplatform.tasks.AbstractLinkTask

class CommonNativePlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.dependencies.attributesSchema {
            it.attribute(NativeArtifact.VARIANT_ATTRIBUTE)
            it.attribute(NativeArtifact.TARGET_ATTRIBUTE)
            it.attribute(NativeArtifact.CONFIGURATION_ATTRIBUTE)
        }

        target.pluginManager.withPlugin('net.derfruhling.spm') {
            def ext = target.extensions.getByType(SpmExtension)
            ext.availableInfoComponents.add(ArtifactUsageInformation)

            target.afterEvaluate {
                ext.configureLazily(target)

                target.tasks.withType(AbstractNativeCompileTask).configureEach {
                    ext.spmInfos.get().forEach { info ->
                        def resolved = info.resolve(ArtifactUsageInformation)
                        if(resolved != null) {
                            resolved.getAll(it.targetPlatform.isPresent() ? NativeTarget.forNativePlatform(it.targetPlatform.get()) : NativeTarget.current)
                                    .collect { c -> c.compileDefinitions }
                                    .forEach { m -> it.macros.putAll(m) }
                        }
                    }
                }

                target.tasks.withType(AbstractLinkTask).configureEach {
                    ext.spmInfos.get().forEach { info ->
                        def resolved = info.resolve(ArtifactUsageInformation)
                        if(resolved != null) {
                            def nativeTarget = it.targetPlatform.isPresent() ? NativeTarget.forNativePlatform(it.targetPlatform.get()) : NativeTarget.current
                            resolved.getAll(nativeTarget)
                                    .collect { c -> c.linkLibraries }
                                    .flatten()
                                    .forEach { m -> it.linkerArgs.add("-l${m}") }

                            if(nativeTarget.platform == 'macos') {
                                resolved.getAll(nativeTarget)
                                        .collect { c -> c.linkFrameworks }
                                        .flatten()
                                        .forEach { m -> it.linkerArgs.addAll("-framework", m as String) }
                            }
                        }
                    }
                }
            }
        }
    }
}
