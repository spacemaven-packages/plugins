package net.derfruhling.cmake


import net.derfruhling.gradle.NativeTarget
import net.derfruhling.gradle.CommonNativePlugin
import net.derfruhling.gradle.NativeArtifact
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.Directory
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.dsl.FileSystemPublishArtifact
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Exec
import org.gradle.language.cpp.CppBinary
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.TargetMachineFactory

import javax.inject.Inject

class CMakeWrapperPlugin implements Plugin<Project> {
    private final ObjectFactory objects
    private final TargetMachineFactory targetMachineFactory
    private final SoftwareComponentFactory componentFactory

    @Inject
    CMakeWrapperPlugin(TargetMachineFactory targetMachineFactory, ObjectFactory objects, SoftwareComponentFactory componentFactory) {
        this.targetMachineFactory = targetMachineFactory
        this.objects = objects
        this.componentFactory = componentFactory
    }

    @Override
    void apply(Project project) {
        project.apply plugin: 'base'
        project.apply plugin: CommonNativePlugin

        project.dependencies.attributesSchema {
            it.attribute(CMake.CMAKE_TARGET_ATTRIBUTE)
        }

        for (final def value in NativeTarget.values()) {
            value.build(targetMachineFactory)
        }

        project.components.registerBinding(AggregateSoftwareComponent, AggregateSoftwareComponent)
        def rootComponent = objects.newInstance(AggregateSoftwareComponent, 'cmake', 'implementation', 'api')
        project.components.add(rootComponent)

        def implementationConfiguration = project.configurations.consumable('implementationElements') {
            it.extendsFrom project.configurations.implementation
        }

        def apiConfiguration = project.configurations.consumable('apiElements') {
            it.extendsFrom project.configurations.api
        }

        def ext = project.extensions.create("cmake", CMakeExtension)
        ext.targets.all()
        ext.variants.defaultVariants()

        project.pluginManager.withPlugin('maven-publish') {
            def publishExt = project.extensions.findByType(PublishingExtension)
            publishExt.publications {
                it.create('cmake', MavenPublication) {
                    it.from rootComponent
                }
            }
        }

        project.afterEvaluate {
            project.pluginManager.withPlugin('maven-publish') {
                def publishExt = project.extensions.findByType(PublishingExtension)
                publishExt.repositories.forEach { repository ->
                    def task = project.tasks.named("publishCmakePublicationTo${repository.name.capitalize()}Repository")

                    def publishTarget = project.tasks.maybeCreate("publishCommonArtifactsTo${repository.name.capitalize()}Repository")
                    publishTarget.dependsOn(task)
                    publishTarget.group = 'publishing'
                    publishTarget.description = "Publish all stub & API artifacts to repository `${repository.name.capitalize()}`"
                }
            }

            rootComponent.publishCoordinates.set(DefaultModuleVersionIdentifier.newId(
                    project.group as String,
                    project.name,
                    project.version as String
            ))

            if(ext.publicHeadersArchive.isPresent()) {
                def component = componentFactory.adhoc('cmakePublicApi')
                def apiConfig = project.configurations.create('cmakePublicApi')
                apiConfig.visible = false

                apiConfig.extendsFrom implementationConfiguration.get(), apiConfiguration.get()

                apiConfig.attributes {
                    it.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                    it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements, LibraryElements.HEADERS_CPLUSPLUS))
                    it.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                    it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.C_PLUS_PLUS_API))
                }

                apiConfig.outgoing {
                    it.artifact(ext.publicHeadersArchive.get().outputs.files.singleFile) {
                        it.builtBy(ext.publicHeadersArchive)
                        it.setClassifier('api')
                    }
                }

                component.addVariantsFromConfiguration(apiConfig) {
                    it.mapToMavenScope("compile")
                }

                rootComponent.registerVariant(component)

                project.pluginManager.withPlugin('maven-publish') {
                    def publishExt = project.extensions.findByType(PublishingExtension)
                    publishExt.publications {
                        it.create(component.name, MavenPublication) {
                            it.from component
                            it.artifactId = project.name + "_api"
                        }
                    }

                    publishExt.repositories.forEach { repository ->
                        def task = project.tasks.named("publish${component.name.capitalize()}PublicationTo${repository.name.capitalize()}Repository")

                        def publishTarget = project.tasks.maybeCreate("publishCommonArtifactsTo${repository.name.capitalize()}Repository")
                        publishTarget.dependsOn(task)
                        publishTarget.group = 'publishing'
                        publishTarget.description = "Publish all stub & API artifacts to repository `${repository.name.capitalize()}`"
                    }
                }
            }

            def number = 0

            ext.variants.variants.get().forEach { variant, parameters ->
                def isDebuggable = parameters.get('isDebuggable', false) as boolean
                def isOptimized = parameters.get('isOptimized', false) as boolean

                ext.targets.targets.get().forEach { cmakeTarget ->
                    ext.configurations.forEach { config ->
                        def definition = config.platforms.get().find { it.target == cmakeTarget }
                        final def capName = config.name.capitalize() + variant.capitalize()

                        final def outputDir = project.layout.buildDirectory.get().dir('cmake').dir(config.name).dir(variant).dir(cmakeTarget.platform).dir(cmakeTarget.architecture)

                        final def configureTask = project.tasks.register("cmake${capName}Configure${cmakeTarget.variantName.capitalize()}", Exec) {
                            it.description = "Configures wrapped CMake configuration '${config.name}'"
                            it.group = 'build'
                            it.inputs.dir(project.fileTree(config.sourceDirectory) {
                                exclude 'build'
                                include 'CMakeLists.txt'
                                include '**/*.cmake'
                            })

                            it.outputs.upToDateWhen { outputDir.asFile.exists() }

                            it.onlyIf { cmakeTarget.isCompatible() }

                            it.commandLine 'cmake',
                                    '-S', config.sourceDirectory.get().asFile.absolutePath,
                                    '-B', outputDir.asFile.absolutePath,
                                    '-G', config.generator.get()

                            if (project.gradle.startParameter.getConsoleOutput() != ConsoleOutput.Plain) {
                                it.environment 'CLICOLOR_FORCE', '1'
                            }

                            it.args config.definitions.get().entrySet().collect { "-D${it.key}=${it.value}" }
                            it.args cmakeTarget.extraCMakeArgs.entrySet().collect { "-D${it.key}=${it.value}" }
                            if(definition != null) it.args definition.platformConfig.definitions.get().entrySet().collect { "-D${it.key}=${it.value}" }
                            it.args config.configureArgs.get().toArray()
                            if(definition != null) it.args definition.platformConfig.configureArgs.get().toArray()
                        }

                        final def configureTaskConfigurer = config.configureTaskCreated.getOrElse(null)
                        if (configureTaskConfigurer != null) configureTask.configure(configureTaskConfigurer)

                        config.targets.forEach { target ->
                            final def outConfig = project.configurations.create("cmakeOut${capName}${variant.capitalize()}${target.name.capitalize()}${cmakeTarget.variantName.capitalize()}")
                            outConfig.visible = false

                            outConfig.extendsFrom implementationConfiguration.get(), apiConfiguration.get()

                            outConfig.attributes {
                                it.attribute(NativeArtifact.CONFIGURATION_ATTRIBUTE, config.name)
                                it.attribute(NativeArtifact.VARIANT_ATTRIBUTE, variant)
                                it.attribute(NativeArtifact.TARGET_ATTRIBUTE, target.name)
                                it.attribute(CMake.CMAKE_TARGET_ATTRIBUTE, cmakeTarget)
                            }

                            def outputKind = target.outputKind.get().name().takeBefore("_").toLowerCase()
                            final def component = componentFactory.adhoc("cmake${capName}${cmakeTarget.variantName.capitalize()}${outputKind.capitalize()}")

                            setupBuildTarget(
                                    cmakeTarget,
                                    target,
                                    outputDir,
                                    project,
                                    capName,
                                    configureTask,
                                    config,
                                    variant,
                                    outConfig,
                                    isDebuggable,
                                    isOptimized
                            )

                            component.addVariantsFromConfiguration(outConfig) {
                                it.mapToOptional()
                            }

                            rootComponent.registerVariant(component)

                            project.pluginManager.withPlugin('maven-publish') {
                                var publishExt = project.extensions.findByType(PublishingExtension)
                                var theName = component.name

                                publishExt.publications {
                                    it.create(theName, MavenPublication) {
                                        it.from component
                                        it.artifactId = "${project.name}_${variant}_${cmakeTarget.platform}_${cmakeTarget.architecture.replace('-', '_')}_${target.name}_${outputKind}"
                                    }
                                }

                                project.tasks.named("generateMetadataFileFor${theName.capitalize()}Publication") {
                                    it.onlyIf { cmakeTarget.isCompatible() }
                                }

                                publishExt.repositories.forEach { repository ->
                                    def task = project.tasks.named("publish${theName.capitalize()}PublicationTo${repository.name.capitalize()}Repository") {
                                        it.onlyIf { cmakeTarget.isCompatible() }
                                    }

                                    def publishTarget = project.tasks.maybeCreate("publish${cmakeTarget.platform.capitalize()}TargetsTo${repository.name.capitalize()}Repository")
                                    publishTarget.dependsOn(task)

                                    publishTarget.doFirst {
                                        if(!cmakeTarget.isCompatible()) {
                                            throw new IllegalStateException("This platform doesn't support publishing these `${cmakeTarget.platform}` artifacts. Did you mean to add this task to a different CI job?")
                                        }
                                    }

                                    publishTarget.group = 'publishing'
                                    publishTarget.description = "Publish all artifacts compatible with the `${cmakeTarget.platform}` platform to repository `${repository.name.capitalize()}`"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void setupBuildTarget(
            NativeTarget cmakeTarget,
            CMakeConfigurationTarget target,
            Directory outputDir,
            Project project,
            String capName,
            Provider<Task> configureTask,
            CMakeConfiguration config,
            String variant,
            Configuration outConfig,
            boolean isDebuggable,
            boolean isOptimized
    ) {
        def artifacts = CMakeExtension.getArtifactName(target.artifactBaseName.get(), target.outputKind.get())
        def definition = config.platforms.get().find { it.target == cmakeTarget }
        artifacts.putAll(definition.platformConfig.outputFileNames.get())
        def linkArtifactFile = outputDir.dir(target.artifactSubDir).map { it.file(artifacts.link) }
        def runtimeArtifactFile = artifacts.containsKey('runtime') ? outputDir.dir(target.artifactSubDir).map { it.file(artifacts.runtime) } : null

        final def buildTask = project.tasks.register("cmake${capName}Build${target.name.capitalize()}${cmakeTarget.variantName.capitalize()}", Exec) {
            it.dependsOn(configureTask)
            it.description = "Builds wrapped CMake target '${target.name}' of configuration '${config.name}'"
            it.group = 'build'
            //it.inputs.files(project.fileTree(config.sourceDirectory) {
            //    exclude 'build'
            //    // random assortment of extensions TODO
            //    include '**/*.{c,cpp,S,asm,cc,h,hpp,hh,ixx,m,mm}'
            //    include 'CMakeLists.txt'
            //})

            it.outputs.file(linkArtifactFile.get())

            if (runtimeArtifactFile != null) {
                it.outputs.file(runtimeArtifactFile.get())
            }

            it.outputs.upToDateWhen { false }

            it.onlyIf { cmakeTarget.isCompatible() }

            it.commandLine 'cmake',
                    '--build', outputDir.asFile.absolutePath,
                    '--target', target.target.get()

            it.environment 'CLICOLOR_FORCE', '1'

            if(definition != null) it.args(definition.platformConfig.buildArgs.get().toArray())
            it.args(config.buildArgs.get().toArray())
        }

        final def buildTaskConfigurer = config.buildTaskCreated.getOrElse(null)
        final def buildTaskConfigurerLocal = target.buildTaskCreated.getOrElse(null)

        if (buildTaskConfigurer != null) buildTask.configure(buildTaskConfigurer)
        if (buildTaskConfigurerLocal != null) buildTask.configure(buildTaskConfigurerLocal)

        def assembleTask = project.tasks.maybeCreate('assemble' + variant.capitalize()).configure {
            it.group = 'build'
            it.description = 'Builds all wrapped CMake targets with variant ' + variant.capitalize()
            it.dependsOn(buildTask)
        }

        project.tasks.assemble.dependsOn(assembleTask)

        outConfig.attributes {
            it.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
            it.attributeProvider(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, target.outputKind.map { objects.named(LibraryElements, it.libraryElementAttribute) })
            it.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, cmakeTarget.machine.operatingSystemFamily)
            it.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, cmakeTarget.machine.architecture)
            it.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
            it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.NATIVE_LINK))
            it.attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, isDebuggable)
            it.attribute(CppBinary.OPTIMIZED_ATTRIBUTE, isOptimized)
            it.attributeProvider(CppBinary.LINKAGE_ATTRIBUTE, target.outputKind.map { it.linkage })
        }

        outConfig.outgoing {
            it.artifact(new FileSystemPublishArtifact(linkArtifactFile.get(), project.version as String) {
                @Override
                boolean shouldBePublished() {
                    return cmakeTarget.isCompatible()
                }
            }) {
                it.builtBy(buildTask)
                it.setClassifier(target.name + '-link-' + variant + cmakeTarget.variantName.capitalize())
            }

            def theVariant = it.variants.maybeCreate("runtime")

            if (runtimeArtifactFile != null) {
                theVariant.artifact(runtimeArtifactFile.get()) {
                    it.builtBy(buildTask)
                    it.setClassifier(target.name + '-runtime-' + variant + cmakeTarget.variantName.capitalize())
                }
            }

            theVariant.attributes {
                it.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                it.attributeProvider(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, target.outputKind.map { objects.named(LibraryElements, it.libraryElementAttribute) })
                it.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, cmakeTarget.machine.operatingSystemFamily)
                it.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, cmakeTarget.machine.architecture)
                it.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.NATIVE_RUNTIME))
                it.attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, isDebuggable)
                it.attribute(CppBinary.OPTIMIZED_ATTRIBUTE, isOptimized)
                it.attributeProvider(CppBinary.LINKAGE_ATTRIBUTE, target.outputKind.map { it.linkage })
            }
        }
    }
}
