package net.derfruhling.spm.publisher

import net.derfruhling.spm.Spm
import net.derfruhling.spm.consumer.InfoConsumerPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

abstract class InfoPublisherPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.plugins.apply(InfoConsumerPlugin)

        def ext = target.extensions.create('spmInfo', SpmInfoExtension)

        ext.outputFile.convention(target.layout.buildDirectory.file('generated/spm-info.xml'))
        ext.artifactBaseName.convention(target.name)

        def buildSpmInfo = target.tasks.register('buildSpmInfo') {
            it.outputs.file(ext.outputFile)

            it.outputs.upToDateWhen { false }

            it.doFirst {
                def outputFile = ext.outputFile.get().asFile
                outputFile.parentFile.mkdirs()

                def builder = new SpmInfoBuilder()

                ext.container.get().forEach { clazz, value ->
                    builder.then(value)
                }

                try(def writer = outputFile.newWriter()) {
                    builder.build(writer)
                }
            }
        }

        def generatedSpmInfo = target.configurations.consumable('generatedSpmInfo') {
            it.attributes {
                it.attribute(Spm.SPM_INFO_ATTRIBUTE, true)
            }
        }

        def spmInfoArtifact = target.artifacts.add('generatedSpmInfo', ext.outputFile) {
            builtBy buildSpmInfo
            name = ext.artifactBaseName.get()
            extension = 'xml'
            type = 'spm+info'
        }

        target.pluginManager.withPlugin('java') {
            (target.components.java as AdhocComponentWithVariants).addVariantsFromConfiguration(generatedSpmInfo.get()) {
                it.mapToOptional()
            }

            /*target.pluginManager.withPlugin('maven-publish') {
                def publishExt = target.extensions.findByType(PublishingExtension)
                (publishExt.publications.java as MavenPublication).artifact(spmInfoArtifact)
            }*/
        }

        target.pluginManager.withPlugin('net.derfruhling.cmake-wrapper') {
            target.components.cmake.registerConfiguration(generatedSpmInfo.get())

            /*target.pluginManager.withPlugin('maven-publish') {
                def publishExt = target.extensions.findByType(PublishingExtension)
                (publishExt.publications.cmake as MavenPublication).artifact(spmInfoArtifact)
            }*/
        }
    }
}
