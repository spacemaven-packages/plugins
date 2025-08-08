package net.derfruhling.gradle;

public record CompilationDatabaseEntry(
        String directory,
        String file,
        String[] arguments
) {
}
