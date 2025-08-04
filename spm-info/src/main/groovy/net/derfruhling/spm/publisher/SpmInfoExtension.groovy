package net.derfruhling.spm.publisher

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

abstract class SpmInfoExtension {
    @Inject
    abstract ObjectFactory getObjects()

    abstract MapProperty<Class<?>, AbstractArtifactInformation> getContainer()
    abstract RegularFileProperty getOutputFile()
    abstract Property<String> getArtifactBaseName()

    <T> void register(@DelegatesTo.Target Class<T> clazz, @DelegatesTo(genericTypeIndex = 0) Closure c) {
        def value = clazz.getAnnotation(Implemented)?.value()?.asSubclass(AbstractArtifactInformation)
        if(value == null) throw new IllegalArgumentException("Not an @Implemented class")

        assert clazz.isAssignableFrom(value)

        def delegateValue = objects.newInstance(value, Optional.empty())
        c.setDelegate(delegateValue)
        c.run()
        container.put(clazz, delegateValue)
    }
}
