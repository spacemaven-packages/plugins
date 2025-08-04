package net.derfruhling.cmake

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.internal.component.ConfigurationSoftwareComponentVariant
import org.gradle.language.ComponentDependencies
import org.gradle.language.ComponentWithDependencies
import org.gradle.language.internal.DefaultLibraryDependencies

import javax.inject.Inject

abstract class AggregateSoftwareComponent implements SoftwareComponent, SoftwareComponentInternal, ComponentWithCoordinates, ComponentWithVariants, ComponentWithDependencies {
    final String name
    final Set<? extends UsageContext> usages = []
    final Set<? extends SoftwareComponent> variants = new HashSet<>()
    final ComponentDependencies dependencies

    @Override
    ComponentDependencies getDependencies() {
        return dependencies
    }

    @Inject
    AggregateSoftwareComponent(String name, ObjectFactory objects, String implementation, String api) {
        this.name = name
        this.dependencies = objects.newInstance(DefaultLibraryDependencies, implementation, api)
    }

    abstract Property<ModuleVersionIdentifier> getPublishCoordinates()

    @Override
    ModuleVersionIdentifier getCoordinates() {
        return publishCoordinates.get()
    }

    void registerUsage(UsageContext ctx) {
        usages.add(ctx)
    }

    void registerConfiguration(Configuration cfg) {
        registerUsage(new ConfigurationSoftwareComponentVariant(cfg.name + 'Registered', cfg.attributes, cfg.artifacts, cfg))
    }

    void registerVariant(SoftwareComponent component) {
        variants.add(component)
    }
}
