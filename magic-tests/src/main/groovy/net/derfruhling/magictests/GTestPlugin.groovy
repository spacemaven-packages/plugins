package net.derfruhling.magictests

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.nativeplatform.test.cpp.plugins.CppUnitTestPlugin
import org.gradle.nativeplatform.test.tasks.RunTestExecutable

class GTestPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.pluginManager.apply(CppUnitTestPlugin)

        target.afterEvaluate {
            target.tasks.withType(RunTestExecutable).forEach { orig ->
                def newTask = target.tasks.register(orig.name + 'GTest', GTestTask) {
                    it.dependsOn(orig.dependsOn.toArray())
                    it.executable.set(target.file(orig.executable))
                    it.extensions.extraProperties.set('idea.internal.test', true)
                    it.binaryOutputDirectory.convention(layout.buildDirectory.dir('test-results/gtest'))
                    it.htmlOutputDirectory.convention(layout.buildDirectory.dir('reports/tests/gtest'))
                    it.outputJUnitReport.convention(it.binaryOutputDirectory.file('output-junit-style.xml'))

                    it.outputs.upToDateWhen { false }
                }

                orig.enabled = false

                target.tasks.test.dependsOn(newTask)
            }
        }
    }
}
