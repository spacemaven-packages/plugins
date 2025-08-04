package net.derfruhling.spm.consumer

import net.derfruhling.spm.Spm
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.specs.Spec

class InfoConsumerPlugin implements Plugin<Project> {
    static void performConfiguration(Project target) {
        def ext = target.extensions.getByType(SpmExtension)
        ext.configureLazily(target)
    }

    @Override
    void apply(Project target) {
        target.dependencies.attributesSchema {
            it.attribute(Spm.SPM_INFO_ATTRIBUTE)
        }

        def ext = target.extensions.create("spm", SpmExtension)

        def addByDefault = [
                'implementation',
                'api',
                'runtimeOnly',
                'compileOnly'
        ]

        ext.configurations.convention(target.provider {
            target.configurations.named(new Spec<String>() {
                @Override
                boolean isSatisfiedBy(String s) {
                    return addByDefault.contains(s)
                }
            })
        })

        target.tasks.register("spmInfo") {
            it.doFirst {
                performConfiguration(target)

                for(def info in ext.spmInfos.get()) {
                    if(info.sourceArtifact != null) {
                        println "from ${info.sourceArtifact}"

                        for (def component in ext.availableInfoComponents.get()) {
                            def value = info.resolve(component)

                            println "->${value.class.name}"

                            System.out.withPrintWriter { writer ->
                                def indentWriter = new IndentWriter(writer, 4)
                                indentWriter.writeValue(value)
                            }
                        }
                    }
                }
            }
        }

        target.afterEvaluate {
            performConfiguration(target)
        }
    }
}
