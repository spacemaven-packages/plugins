//file:noinspection GrUnresolvedAccess
package net.derfruhling.spm.consumer

import groovy.xml.XmlParser
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import net.derfruhling.spm.publisher.Implemented
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.model.ObjectFactory

import javax.annotation.Nullable
import javax.inject.Inject

class SpmInfo {
    public static final String NAMESPACE = 'https://spacemaven-packages.github.io/schemas/SpmInfo.xsd'

    private final ObjectFactory objects
    private final GPathResult xml

    final String sourceArtifact

    @Inject
    SpmInfo(ObjectFactory objects, String sourceArtifact, Reader reader) {
        this.objects = objects

        def parser = new XmlSlurper()
        def result = parser.parse(reader)

        result.declareNamespace SpmInfo: NAMESPACE

        xml = result
        this.sourceArtifact = sourceArtifact
    }

    @Nullable
    <T> T resolve(Class<T> clazz) {
        def consumes = clazz.getAnnotationsByType(Consumes)

        for(def annot in consumes) {
            xml.declareNamespace((annot.namespace()): annot.namespaceUri())
            def result = xml.getProperty("${annot.namespace()}:${annot.elementName()}") as GPathResult

            if(result != null) {
                def implClazz = clazz.getAnnotation(Implemented)?.value()?.asSubclass(clazz)
                if(implClazz == null) continue
                def obj = objects
                return obj.newInstance(implClazz, Optional.of(result))
            }
        }

        return null
    }
}
