package net.derfruhling.cmake

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.attributes.AbstractAttributeContainer
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.provider.Provider

import javax.inject.Inject

class AttributesInternal implements AttributeContainer {
    protected Map<Attribute<?>, Provider<?>> map = new HashMap<>()

    @Inject
    AttributesInternal() {}

    @Override
    Set<Attribute<?>> keySet() {
        return map.keySet()
    }

    @Override
    <T> AttributeContainer attribute(Attribute<T> key, T value) {
        map[key] = new DefaultProvider<T>({ value })
        return this
    }

    @Override
    <T> AttributeContainer attributeProvider(Attribute<T> key, Provider<? extends T> provider) {
        map[key] = provider
        return null
    }

    @Override
    <T> T getAttribute(Attribute<T> key) {
        return map[key].get() as T
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        AttributesInternal that = (AttributesInternal) o

        if (map != that.map) return false

        return true
    }

    int hashCode() {
        return (map != null ? map.hashCode() : 0)
    }

    @Override
    boolean isEmpty() {
        return map.isEmpty()
    }

    @Override
    boolean contains(Attribute<?> key) {
        return map.containsKey(key)
    }

    @Override
    AttributeContainer getAttributes() {
        return this
    }

    AttributesInternal then(AttributesInternal other) {
        return new AttributesInternal(map + other.map)
    }

    AttributesInternal then(Map<Attribute<?>, ?> other) {
        return new AttributesInternal(map + other)
    }
}
