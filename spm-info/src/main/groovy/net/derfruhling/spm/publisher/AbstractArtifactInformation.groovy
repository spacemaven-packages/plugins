package net.derfruhling.spm.publisher

import groovy.xml.MarkupBuilder

abstract class AbstractArtifactInformation {
    protected AbstractArtifactInformation() {
    }

    interface NamespaceImporter {
        void importNamespace(String name, String url)
    }

    abstract void importNamespaces(NamespaceImporter ns)
    abstract void write(MarkupBuilder builder)
}
