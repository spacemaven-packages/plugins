package net.derfruhling.spm.publisher

import groovy.xml.MarkupBuilder
import net.derfruhling.spm.consumer.SpmInfo

class SpmInfoBuilder {
    private final Map<Class<? extends AbstractArtifactInformation>, AbstractArtifactInformation> parts = [:]
    private static final MODEL_VERSION = 1

    SpmInfoBuilder then(AbstractArtifactInformation information) {
        parts.put(information.class, information)
        return this
    }

    void build(Writer writer) {
        MarkupBuilder builder = new MarkupBuilder(writer)

        def attributes = ['xmlns:SpmInfo': SpmInfo.NAMESPACE]

        for(def part in parts.values()) {
            part.importNamespaces { ns, url ->
                attributes.put("xmlns:" + ns, url)
            }
        }

        builder.mkp.pi 'xml': [version: '1.0', encoding: 'UTF-8']
        builder.mkp.pi 'spm': [model: MODEL_VERSION]

        builder.'SpmInfo:Declare'(attributes) {
            for(def part in parts) {
                part.value.write(builder)
            }
        }
    }
}
