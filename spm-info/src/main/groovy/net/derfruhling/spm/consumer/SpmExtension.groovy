package net.derfruhling.spm.consumer

import net.derfruhling.spm.Spm
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.language.c.tasks.CCompile
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask

import javax.inject.Inject

abstract class SpmExtension {
    abstract ListProperty<Configuration> getConfigurations()
    abstract ListProperty<SpmInfo> getSpmInfos()
    abstract ListProperty<Class<?>> getAvailableInfoComponents()

    private final Property<Boolean> isConfigured

    @Inject
    SpmExtension(ObjectFactory objects) {
        isConfigured = objects.property(Boolean)
        isConfigured.set(false)
    }

    final void configureLazily(Project project) {
        if(isConfigured.get() || project.configurations.hasProperty('spmInfoResolve')) return
        def collection = project.configurations.resolvable('spmInfoResolve').get()

        collection.canBeResolved = true
        collection.canBeConsumed = false
        collection.canBeDeclared = false

        collection.extendsFrom(configurations.get().toArray(Configuration[]::new))

        collection.resolutionStrategy {
            componentSelection {
                it.all {
                    if(metadata.attributes.getAttribute(Spm.SPM_INFO_ATTRIBUTE) != true) {
                        reject('not a spm info document')
                    }
                }
            }
        }

        def view = collection.incoming.artifactView {
            it.withVariantReselection()
            it.lenient(true)
            it.attributes {
                it.attribute(Spm.SPM_INFO_ATTRIBUTE, true)
            }
        }

        def infos = view.artifacts.artifacts.collect {
            it.file.withReader { reader ->
                project.objects.newInstance(SpmInfo, it.id.toString(), reader)
            }
        }

        spmInfos.value(infos)
        isConfigured.set(true)
    }
}
