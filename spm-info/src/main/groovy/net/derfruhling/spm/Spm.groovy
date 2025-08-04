package net.derfruhling.spm

import org.gradle.api.attributes.Attribute

class Spm {
    public static final Attribute<Boolean> SPM_INFO_ATTRIBUTE = Attribute.of("net.derfruhling.spm-info", Boolean)

    private Spm() {}
}
