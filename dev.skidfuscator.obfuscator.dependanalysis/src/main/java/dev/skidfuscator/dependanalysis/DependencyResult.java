package dev.skidfuscator.dependanalysis;

import lombok.Data;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DependencyResult provides a structured representation of the required dependencies.
 * Each JarDependency represents an external jar that is needed, containing the classes
 * from that jar that are required and reasons for their necessity.
 */
@Getter
public class DependencyResult {
    private final List<JarDependency> jarDependencies;

    public DependencyResult(List<JarDependency> jarDependencies) {
        this.jarDependencies = jarDependencies;
    }

    public void printReport() {
        System.out.println("Required Dependencies:");
        System.out.println("=====================\n");

        if (this.getJarDependencies().isEmpty()) {
            System.out.println("No external dependencies are required.");
            return;
        }

        for (DependencyResult.JarDependency jarDependency : this.getJarDependencies()) {
            System.out.println("JAR: " + jarDependency.getJarPath().getFileName());
            System.out.println("---------------------------------------------------");
            for (DependencyResult.ClassDependency classDep : jarDependency.getClassesNeeded()) {
                System.out.println("  Class: " + classDep.getClassName());
                for (String reason : classDep.getReasons()) {
                    System.out.println("    - " + reason);
                }
            }
            System.out.println();
        }
    }
    @Data
    public static class JarDependency {
        private final Path jarPath;
        private final List<ClassDependency> classesNeeded;

        @Override
        public String toString() {
            return "[" + this.getJarPath().getFileName() + "]"
                    + "\nClasses [" + this.getClassesNeeded().size() + "]:"
                    + "\n" + this.getClassesNeeded().stream()
                    .map(e -> "  - " + e.getClassName() + " (" + String.join(", ", e.getReasons()) + ")")
                    .collect(Collectors.joining("\n"))
                    ;
        }
    }

    @Data
    public static class ClassDependency {
        private final String className;
        private final List<String> reasons;
    }
}
