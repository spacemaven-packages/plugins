package net.derfruhling.gradle

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

abstract class AggregateJsonTask extends DefaultTask {
    @InputFiles
    @SkipWhenEmpty
    abstract ConfigurableFileCollection getInputFiles();

    @OutputFile
    abstract RegularFileProperty getOutputFile();

    @TaskAction
    void act() {
        var gson = new GsonBuilder()
                .setPrettyPrinting()
                .create()

        var input = getInputFiles().files
                .collect { gson.fromJson(it.text, new TypeToken<List<CompilationDatabaseEntry>>() {}) }
                .flatten() as List<CompilationDatabaseEntry>

        getOutputFile().get().asFile.write(gson.toJson(input))
    }
}
