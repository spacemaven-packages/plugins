package net.derfruhling.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask;
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform;
import org.jetbrains.annotations.NotNull;

public class CompileCommandsPlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        target.getTasks().withType(CppCompile.class).whenTaskAdded(relevantCompileTask -> {
            target.getTasks().register(relevantCompileTask.getName() + "Commands", CompileCommandsTask.class, task -> {
                task.getSources().set(relevantCompileTask.getSource());
                task.getCompileArgs().set(relevantCompileTask.getCompilerArgs());
                var os = relevantCompileTask.getTargetPlatform().getOrElse(DefaultNativePlatform.host()).getOperatingSystem();

                if(os.isWindows()) {
                    task.getCompiler().set("cl");
                } else if(os.isMacOsX()) {
                    task.getCompiler().set("clang");
                } else if(os.isLinux()) {
                    task.getCompiler().set("gcc");
                }

                task.getOutputFile().set(relevantCompileTask.getObjectFileDir().file("compile_commands.json"));
                task.getRootPath().set(target.getLayout().getProjectDirectory().getAsFile().getAbsolutePath());
            });
        });
    }
}
