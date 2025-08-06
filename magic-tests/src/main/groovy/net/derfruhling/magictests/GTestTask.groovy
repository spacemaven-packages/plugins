package net.derfruhling.magictests

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.TestEventReporterFactory
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.workers.WorkerExecutionException
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

abstract class GTestTask extends DefaultTask {
    @Inject
    abstract ProjectLayout getLayout()

    @Inject
    protected abstract TestEventReporterFactory getTestEventReporterFactory()

    @Inject
    protected abstract ExecOperations getExecOperations()

    @InputFile
    abstract RegularFileProperty getExecutable()

    @OutputFile
    abstract RegularFileProperty getOutputJUnitReport()

    @OutputDirectory
    abstract DirectoryProperty getBinaryOutputDirectory()

    @OutputDirectory
    abstract DirectoryProperty getHtmlOutputDirectory()

    @TaskAction
    void runTask() {
        try(
                def root = testEventReporterFactory.createTestEventReporter(
                        'root',
                        layout.buildDirectory.dir('test-results/gtest').get(),
                        layout.buildDirectory.dir('reports/tests/gtest').get()
                )
                def receiver = new GTestStreamingReceiver()
        ) {
            Process process = null

            receiver.accept(root) {
                root.started(Instant.now())
                process = Runtime.getRuntime().exec(new String[] {
                        executable.get().asFile.absolutePath,
                        '--gtest_brief=1',
                        "--gtest_stream_result_to=127.0.0.1:${receiver.port}",
                        "--gtest_output=xml:${outputJUnitReport.get().asFile.absolutePath}",
                        '--gtest_color=no'
                })

                process.consumeProcessOutput(System.out, System.err)
            }

            if(process.waitFor() == 0) {
                root.succeeded(Instant.now())
            } else {
                root.failed(Instant.now())
            }
        }
    }
}
