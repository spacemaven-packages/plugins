package net.derfruhling.magictests

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

import javax.inject.Inject

abstract class GTestExecutor implements WorkAction<Parameters> {
    @Inject
    protected abstract ExecOperations getExecOperations()

    @Override
    void execute() {

    }

    interface Parameters extends WorkParameters {
        RegularFileProperty getExecutable()
        Property<Integer> getPort()
    }
}
