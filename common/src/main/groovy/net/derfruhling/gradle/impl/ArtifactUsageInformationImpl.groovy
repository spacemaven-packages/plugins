package net.derfruhling.gradle.impl

import groovy.xml.MarkupBuilder
import groovy.xml.slurpersupport.GPathResult
import net.derfruhling.gradle.ArtifactUsageInformation
import net.derfruhling.gradle.NativeTarget
import net.derfruhling.spm.consumer.IndentWriter
import net.derfruhling.spm.consumer.Printable
import net.derfruhling.spm.publisher.AbstractArtifactInformation

import javax.inject.Inject

class ArtifactUsageInformationImpl extends AbstractArtifactInformation implements ArtifactUsageInformation {
    static class ArtifactUsageContext implements ArtifactUsageInformation.ArtifactUsageContext {
        final Map<String, String> compileDefinitions = [:]
        final List<String> linkLibraries = []
        final List<String> linkFrameworks = []

        ArtifactUsageContext() {}

        @SuppressWarnings('GrUnresolvedAccess')
        ArtifactUsageContext(GPathResult data) {
            def compileDefsElement = data.'ArtifactUsage:CompileDefinitions' as GPathResult
            if(compileDefsElement != null) {
                for(def compileDef in compileDefsElement.'ArtifactUsage:AddCompileDefinition') {
                    def name = compileDef.'ArtifactUsage:Name' as String
                    def value = compileDef.'ArtifactUsage:Value' as String

                    compileDefinitions[name] = value
                }
            }

            def linkLibrariesElement = data.'ArtifactUsage:LinkLibraries' as GPathResult
            if(linkLibrariesElement != null) {
                for(def linkLibrary in linkLibrariesElement.'ArtifactUsage:PreExistingLibrary') {
                    if(linkLibrary.isEmpty()) continue
                    linkLibraries.add(linkLibrary as String)
                }
            }

            def linkFrameworksElement = data.'ArtifactUsage:LinkFrameworks' as GPathResult
            if(linkFrameworksElement != null) {
                for(def linkLibrary in linkFrameworksElement.'ArtifactUsage:PreExistingFramework') {
                    if(linkLibrary.isEmpty()) continue
                    linkLibraries.add(linkLibrary as String)
                }
            }
        }

        void expectLibrary(String... libraries) {
            linkLibraries.addAll(libraries)
        }

        void expectFramework(String... frameworks) {
            linkFrameworks.addAll(frameworks)
        }

        void requireCompileDefinition(Map<String, ?> newDefs) {
            for(def v in newDefs) {
                compileDefinitions.put(v.key, v.value?.toString() ?: 'null')
            }
        }

        boolean isEmpty() {
            return compileDefinitions.isEmpty() && linkLibraries.isEmpty()
        }

        void write(MarkupBuilder builder) {
            if(!compileDefinitions.isEmpty()) {
                builder.'ArtifactUsage:CompileDefinitions'() {
                    for(def v in compileDefinitions) {
                        'ArtifactUsage:AddCompileDefinition'() {
                            'ArtifactUsage:Name'(v.key)
                            'ArtifactUsage:Value'(v.value)
                        }
                    }
                }
            }

            if(!linkLibraries.isEmpty()) {
                builder.'ArtifactUsage:LinkLibraries'() {
                    for(def v in linkLibraries) {
                        'ArtifactUsage:PreExistingLibrary'(v)
                    }
                }
            }

            if(!linkFrameworks.isEmpty()) {
                builder.'ArtifactUsage:LinkFrameworks'() {
                    for(def v in linkFrameworks) {
                        'ArtifactUsage:PreExistingFramework'(v)
                    }
                }
            }
        }

        @Override
        void prettyPrintTo(IndentWriter writer) {
            writer.writeProperty compileDefinitions: compileDefinitions,
                                 linkLibraries: linkLibraries,
                                 linkFrameworks: linkFrameworks
        }
    }

    ArtifactUsageContext all = new ArtifactUsageContext()
    Map<List<NativeTarget>, ArtifactUsageContext> platforms = [:]

    private setAll(ArtifactUsageContext c) { all = c }
    private setPlatforms(Map<List<NativeTarget>, ArtifactUsageContext> c) { platforms = c }

    @Override
    void forAll(@DelegatesTo(ArtifactUsageContext) Closure closure) {
        closure.setDelegate(all)
        closure.run()
    }

    @Override
    void platform(List<NativeTarget> targets, @DelegatesTo(ArtifactUsageContext) Closure closure) {
        closure.setDelegate(platforms.computeIfAbsent(targets) { new ArtifactUsageContext() })
        closure.run()
    }

    @Override
    List<ArtifactUsageContext> getAll(NativeTarget target) {
        return platforms.findAll { it.key.contains(target) }.values().toList() + all
    }

    ArtifactUsageInformationImpl() {}

    @SuppressWarnings('GrUnresolvedAccess')
    @Inject
    ArtifactUsageInformationImpl(Optional<GPathResult> opt) {
        if(opt.isPresent()) {
            def data = opt.get()
            def allElement = data.'ArtifactUsage:All'
            if(allElement != null) all = new ArtifactUsageContext(allElement as GPathResult)

            this.platforms = [:]

            def platforms = data.'ArtifactUsage:Platform'
            for(def platform in platforms) {
                def onlyPlatformString = platform.'@ArtifactUsage:OnlyPlatforms' as String
                def onlyPlatforms = onlyPlatformString.split(',').collect { NativeTarget.valueOf(it.toUpperCase().trim()) }
                this.platforms[onlyPlatforms] = new ArtifactUsageContext(platform as GPathResult)
            }
        }
    }

    @Override
    void importNamespaces(NamespaceImporter ns) {
        ns.importNamespace 'ArtifactUsage', NAMESPACE
    }

    @Override
    void write(MarkupBuilder builder) {
        builder.'ArtifactUsage:Definition'() {
            if(!all.isEmpty()) {
                'ArtifactUsage:All'() {
                    all.write(builder)
                }
            }

            for(def platform in platforms) {
                'ArtifactUsage:Platform'(
                        'ArtifactUsage:OnlyPlatforms': platform.key.collect {
                            it.toString().toLowerCase()
                        }.join(',')
                ) {
                    platform.value.write(builder)
                }
            }
        }
    }

    @Override
    void prettyPrintTo(IndentWriter writer) {
        writer.writeProperty $namespace: NAMESPACE,
                             all: all,
                             platforms: platforms
    }
}
