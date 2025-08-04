package net.derfruhling.gradle


import net.derfruhling.gradle.impl.ArtifactUsageInformationImpl
import net.derfruhling.spm.consumer.Consumes
import net.derfruhling.spm.consumer.Printable
import net.derfruhling.spm.publisher.Implemented

@Implemented(ArtifactUsageInformationImpl)
@Consumes(
        namespace = "ArtifactUsage",
        namespaceUri = 'https://spacemaven-packages.github.io/schemas/ArtifactUsageInformation.xsd',
        elementName = "Definition"
)
trait ArtifactUsageInformation extends Printable {
    final String NAMESPACE = 'https://spacemaven-packages.github.io/schemas/ArtifactUsageInformation.xsd'

    static trait ArtifactUsageContext extends Printable {
        abstract Map<String, String> getCompileDefinitions()
        abstract List<String> getLinkLibraries()
        abstract List<String> getLinkFrameworks()

        abstract void expectLibrary(String... libraries)
        abstract void expectFramework(String... frameworks)
        abstract void requireCompileDefinition(Map<String, ?> newDefs)
    }

    abstract void forAll(@DelegatesTo(ArtifactUsageContext) Closure closure)
    abstract void platform(List<NativeTarget> targets, @DelegatesTo(ArtifactUsageContext) Closure closure)

    abstract List<ArtifactUsageContext> getAll(NativeTarget target)

    void platform(NativeTarget target, @DelegatesTo(ArtifactUsageContext) Closure closure) {
        platform([target], closure)
    }

    void platform(NativeTarget t1, NativeTarget t2, @DelegatesTo(ArtifactUsageContext) Closure closure) {
        platform([t1, t2], closure)
    }

    void platform(NativeTarget t1, NativeTarget t2, NativeTarget t3, @DelegatesTo(ArtifactUsageContext) Closure closure) {
        platform([t1, t2, t3], closure)
    }

    void platform(NativeTarget t1, NativeTarget t2, NativeTarget t3, NativeTarget t4, @DelegatesTo(ArtifactUsageContext) Closure closure) {
        platform([t1, t2, t3, t4], closure)
    }
}
