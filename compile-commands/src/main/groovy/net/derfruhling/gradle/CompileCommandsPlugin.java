package net.derfruhling.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.cpp.CppLibrary;
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask;
import org.gradle.nativeplatform.NativeComponentExtension;
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class CompileCommandsPlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        var aggregateCompileCommands = target.getTasks().register("aggregateCompileCommands", AggregateJsonTask.class, task -> {
            task.getOutputFile().set(target.file("compile_commands.json"));
        });

        for (Project subProj : target.getSubprojects()) {
            configureProject(subProj, aggregateCompileCommands);
        }

        configureProject(target, aggregateCompileCommands);
    }

    private static void configureProject(Project subProj, TaskProvider<AggregateJsonTask> aggregateCompileCommands) {
        subProj.getTasks().withType(AbstractNativeCompileTask.class).whenTaskAdded(relevantCompileTask -> {
            var newTask = subProj.getTasks().register(relevantCompileTask.getName() + "Commands", CompileCommandsTask.class, task -> {
                task.getSources().set(relevantCompileTask.getSource());
                task.getObjectDir().set(relevantCompileTask.getObjectFileDir().map(v -> v.getAsFile().getAbsolutePath()));
                task.getCompileArgs().addAll(relevantCompileTask.getCompilerArgs());
                task.getCompileArgs().addAll(relevantCompileTask.getMacros().entrySet().stream().map(e -> "-D" + e.getKey() + "=" + e.getValue()).toList());
                task.getCompileArgs().addAll(relevantCompileTask.getIncludes().getFiles().stream().map(l -> "-I" + l.getAbsolutePath()).toList());
                task.getCompileArgs().addAll(relevantCompileTask.getSystemIncludes().getFiles().stream().flatMap(l -> Stream.of("-isystem", l.getAbsolutePath())).toList());
                var os = relevantCompileTask.getTargetPlatform().getOrElse(DefaultNativePlatform.host()).getOperatingSystem();

                if(os.isWindows()) {
                    task.getCompiler().set("cl");
                } else if(os.isMacOsX()) {
                    task.getCompiler().set("clang");
                } else if(os.isLinux()) {
                    task.getCompiler().set("gcc");
                }

                task.getOutputFile().set(relevantCompileTask.getObjectFileDir().file("compile_commands.json"));
                task.getRootPath().set(subProj.getLayout().getProjectDirectory().getAsFile().getAbsolutePath());
            });

            aggregateCompileCommands.configure(task -> task.getInputFiles().from(newTask));
        });
    }
}
