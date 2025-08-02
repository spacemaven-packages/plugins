package net.derfruhling.cmake

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.language.cpp.CppBinary
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.TargetMachineFactory

import javax.inject.Inject

class CMakeWrapperPlugin implements Plugin<Project> {
    private final ObjectFactory objects
    private final TargetMachineFactory targetMachineFactory
    private final SoftwareComponentFactory componentFactory

    static final BUILT_BY_CMAKE_ATTRIBUTE = Attribute.of('net.derfruhling.cmake-wrapper', Boolean)

    @Inject
    CMakeWrapperPlugin(TargetMachineFactory targetMachineFactory, ObjectFactory objects, SoftwareComponentFactory componentFactory) {
        this.targetMachineFactory = targetMachineFactory
        this.objects = objects
        this.componentFactory = componentFactory
    }

    @Override
    void apply(Project project) {
        project.apply plugin: 'base'

        for (final def value in CMakeTarget.values()) {
            value.build(targetMachineFactory)
        }

        def implementationConfiguration = project.configurations.maybeCreate("implementation")
        def apiConfiguration = project.configurations.maybeCreate("api")
        def runtimeOnlyConfiguration = project.configurations.maybeCreate("runtimeOnly")
        def compileOnlyConfiguration = project.configurations.maybeCreate("compileOnly")

        def component = componentFactory.adhoc('cmake')
        project.components.add(component)

        def ext = project.extensions.create("cmake", CMakeExtension)
        ext.targets.all()
        ext.variants.defaultVariants()

        def outConfig = project.configurations.create('cmakeOutput')

        project.afterEvaluate {
            if(ext.publicHeadersArchive.isPresent()) {
                outConfig.outgoing {
                    def theVariant = it.variants.create('api')
                    theVariant.attributes {
                        it.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                        it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements, LibraryElements.HEADERS_CPLUSPLUS))
                        it.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                        it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.C_PLUS_PLUS_API))
                    }

                    theVariant.artifact(ext.publicHeadersArchive.get().outputs.files.singleFile) {
                        it.builtBy(ext.publicHeadersArchive)
                        it.setClassifier('api')
                    }
                }
            }

            def allComponents = new ArrayList<SoftwareComponent>()
            allComponents.add(component)

            ext.variants.variants.get().forEach { variant, parameters ->
                def isDebuggable = parameters.get('isDebuggable', false) as boolean
                def isOptimized = parameters.get('isOptimized', false) as boolean

                ext.configurations.forEach { config ->
                    final def capName = config.name.capitalize() + variant.capitalize()
                    final def outputDir = project.layout.buildDirectory.get().dir('cmake').dir(config.name).dir(variant)

                    final def configureTask = project.tasks.register("cmake${capName}Configure", Exec) {
                        it.description = "Configures wrapped CMake configuration '${config.name}'"
                        it.group = 'build'
                        it.inputs.dir(project.fileTree(config.sourceDirectory) {
                            exclude 'build'
                            include 'CMakeLists.txt'
                            include '**/*.cmake'
                        })

                        it.outputs.upToDateWhen { outputDir.asFile.exists() }

                        it.commandLine 'cmake',
                                '-S', config.sourceDirectory.get().asFile.absolutePath,
                                '-B', outputDir.asFile.absolutePath

                        if (project.gradle.startParameter.getConsoleOutput() != ConsoleOutput.Plain) {
                            it.environment 'CLICOLOR_FORCE', '1'
                        }

                        it.args config.definitions.get().entrySet().collect { "-D${it.key}=${it.value}" }
                        it.args config.configureArgs.get().toArray()
                    }

                    final def configureTaskConfigurer = config.configureTaskCreated.getOrElse(null)

                    if (configureTaskConfigurer != null) configureTask.configure(configureTaskConfigurer)

                    config.targets.forEach { target ->
                        def artifacts = CMakeExtension.getArtifactName(target.artifactBaseName.get(), target.outputKind.get())
                        def linkArtifactFile = outputDir.dir(target.artifactSubDir).map { it.file(artifacts.link) }
                        def runtimeArtifactFile = artifacts.containsKey('runtime') ? outputDir.dir(target.artifactSubDir).map { it.file(artifacts.runtime) } : null

                        final def buildTask = project.tasks.register("cmake${capName}Build${target.name.capitalize()}", Exec) {
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

                            it.commandLine 'cmake',
                                    '--build', outputDir.asFile.absolutePath,
                                    '--target', target.target.get()

                            it.environment 'CLICOLOR_FORCE', '1'

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

                        for (final def cmakeTarget in CMakeTarget.values()) {
                            def cmakeTargetBaseName = "cmake${config.name.capitalize()}${variant.capitalize()}${target.name.capitalize()}${cmakeTarget.variantName.capitalize()}"

                            /* Using maybeCreate here allows the configurations
                               to depend on information defined in the extension
                               while also allowing special cases of very specific
                               output targets requiring special configuration */

                            /*
                            // example of what we're trying to support
                            configurations {
                                cmakeVariantTargetPlatformArchitecture // common elements
                                cmakeVariantTargetPlatformArchitectureLink // linking elements
                                cmakeVariantTargetPlatformArchitectureRuntime // runtime elements

                                // examples
                                cmakeReleaseGtestWindowsX86_64
                                cmakeReleaseGtestWindowsX86_64Link
                                cmakeReleaseGtestWindowsX86_64Runtime

                                // these then should be usable as regular dependency configurations
                            }
                            */

                            def commonConfig = project.configurations.maybeCreate(cmakeTargetBaseName)
                            commonConfig.extendsFrom(implementationConfiguration, apiConfiguration)

                            def linkConfig = project.configurations.maybeCreate(cmakeTargetBaseName + 'Link')
                            linkConfig.extendsFrom(commonConfig, compileOnlyConfiguration)

                            def runtimeConfig = project.configurations.maybeCreate(cmakeTargetBaseName + 'Runtime')
                            runtimeConfig.extendsFrom(commonConfig, runtimeOnlyConfiguration)

                            outConfig.outgoing {
                                def theVariant = it.variants.maybeCreate("${project.name}_${variant}_${cmakeTarget.platform}_${cmakeTarget.architecture.replace('-', '_')}_${target.outputKind.get().name().toLowerCase().takeBefore("_")}")

                                theVariant.artifact(linkArtifactFile.get()) {
                                    it.builtBy(buildTask)
                                    it.setClassifier(target.name + '-link-' + variant)
                                }

                                theVariant.attributes {
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

                                if(runtimeArtifactFile != null) {
                                    theVariant.artifact(runtimeArtifactFile.get()) {
                                        it.builtBy(buildTask)
                                        it.setClassifier(target.name + '-runtime-' + variant)
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

                            /*def linkArtifact = project.artifacts.add(linkElementsConfig.name, linkArtifactFile.get()) {
                                setName("${ConfigurablePublishArtifact.getName()}_${variant}_${cmakeTarget.platform}_${cmakeTarget.architecture.replace('-', '_')}_${target.outputKind.get().name().toLowerCase().takeBefore("_")}")
                                setClassifier("${cmakeTarget.platform}-${cmakeTarget.architecture}-${variant}-${target.outputKind.get().name().toLowerCase().takeBefore("_")}-link")
                            }*/

                            /*def runtimeArtifact = runtimeArtifactFile != null
                                    ? project.artifacts.add(runtimeElementsConfig.name, runtimeArtifactFile.get()) {
                                setName("${ConfigurablePublishArtifact.getName()}_${variant}_${cmakeTarget.platform}_${cmakeTarget.architecture.replace('-', '_')}_${target.outputKind.get().name().toLowerCase().takeBefore("_")}")
                                setClassifier("${cmakeTarget.platform}-${cmakeTarget.architecture}-${variant}-${target.outputKind.get().name().toLowerCase().takeBefore("_")}-runtime")
                            } : null*/
                        }
                    }
                }
            }

            outConfig.attributes {
                it.attribute(BUILT_BY_CMAKE_ATTRIBUTE, true)
            }

            component.addVariantsFromConfiguration(outConfig) {
                it.mapToOptional()
            }
        }
    }
}
