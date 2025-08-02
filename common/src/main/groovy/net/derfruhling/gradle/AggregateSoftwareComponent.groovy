package net.derfruhling.gradle

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.provider.Property

import javax.inject.Inject

abstract class AggregateSoftwareComponent implements SoftwareComponent, SoftwareComponentInternal, ComponentWithCoordinates, ComponentWithVariants {
    final String name
    final Set<? extends UsageContext> usages = []
    final Set<? extends SoftwareComponent> variants = new HashSet<>()

    @Inject
    AggregateSoftwareComponent(String name) {
        this.name = name
    }

    abstract Property<ModuleVersionIdentifier> getPublishCoordinates()

    @Override
    ModuleVersionIdentifier getCoordinates() {
        return publishCoordinates.get()
    }

    void registerVariant(SoftwareComponent component) {
        variants.add(component)
    }
}
