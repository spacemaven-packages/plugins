package net.derfruhling.gradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public abstract class CompileCommandsTask extends DefaultTask {
    public record CompilationDatabaseEntry(
            String directory,
            String file,
            String[] arguments
    ) {}

    @Input
    public abstract Property<String> getRootPath();

    @Input
    public abstract ListProperty<String> getCompileArgs();

    @Input
    public abstract Property<String> getCompiler();

    @Input
    public abstract Property<FileCollection> getSources();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    @TaskAction
    void act() throws IOException {
        var entries = getSources().get().getFiles().stream()
            .map(file -> {
                var compileArgs = new ArrayList<String>();
                compileArgs.add(getCompiler().get());
                compileArgs.addAll(getCompileArgs().get());
                compileArgs.add("-o");
                compileArgs.add("-c");
                compileArgs.add(file.getPath() + ".o");
                compileArgs.add(file.getPath());
                return new CompilationDatabaseEntry(
                        getRootPath().get(),
                        file.getAbsolutePath(),
                        compileArgs.toArray(String[]::new)
                );
            })
            .toList();

        var json = gson.toJson(entries);

        Files.createDirectories(getOutputFile().getAsFile().get().getParentFile().toPath());
        Files.writeString(getOutputFile().getAsFile().get().toPath(), json);
    }
}
