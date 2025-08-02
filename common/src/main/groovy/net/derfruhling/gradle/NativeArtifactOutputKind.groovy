package net.derfruhling.gradle

import org.gradle.api.attributes.LibraryElements
import org.gradle.nativeplatform.Linkage

enum NativeArtifactOutputKind {
    STATIC_LIBRARY(LibraryElements.LINK_ARCHIVE, Linkage.STATIC),
    SHARED_LIBRARY(LibraryElements.DYNAMIC_LIB, Linkage.SHARED)

    final String libraryElementAttribute
    final Linkage linkage

    NativeArtifactOutputKind(String libraryElementAttribute, Linkage linkage) {
        this.libraryElementAttribute = libraryElementAttribute
        this.linkage = linkage
    }
}