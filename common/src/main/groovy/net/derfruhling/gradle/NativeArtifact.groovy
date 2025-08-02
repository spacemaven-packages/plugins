package net.derfruhling.gradle

import org.gradle.api.attributes.Attribute

final class NativeArtifact {
    static final VARIANT_ATTRIBUTE = Attribute.of('net.derfruhling.variant', String)
    static final CONFIGURATION_ATTRIBUTE = Attribute.of('net.derfruhling.configuration', String)
    static final TARGET_ATTRIBUTE = Attribute.of('net.derfruhling.target', String)

    private NativeArtifact() {}
}
