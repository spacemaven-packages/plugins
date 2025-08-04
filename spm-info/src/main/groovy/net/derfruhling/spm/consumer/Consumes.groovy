package net.derfruhling.spm.consumer

import kotlin.annotation.MustBeDocumented

import java.lang.annotation.ElementType
import java.lang.annotation.Repeatable
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MustBeDocumented
@interface Consumes {
    String namespace()
    String namespaceUri()
    String elementName()
}
