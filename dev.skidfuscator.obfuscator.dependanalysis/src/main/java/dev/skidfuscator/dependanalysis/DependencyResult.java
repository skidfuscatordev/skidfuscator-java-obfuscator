package dev.skidfuscator.dependanalysis;

import java.nio.file.Path;
import java.util.List;

/**
 * DependencyResult provides a structured representation of the required dependencies.
 * Each JarDependency represents an external jar that is needed, containing the classes
 * from that jar that are required and reasons for their necessity.
 */
public class DependencyResult {
    private final List<JarDependency> jarDependencies;

    public DependencyResult(List<JarDependency> jarDependencies) {
        this.jarDependencies = jarDependencies;
    }

    public List<JarDependency> getJarDependencies() {
        return jarDependencies;
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
    public static class JarDependency {
        private final Path jarPath;
        private final List<ClassDependency> classesNeeded;

        public JarDependency(Path jarPath, List<ClassDependency> classesNeeded) {
            this.jarPath = jarPath;
            this.classesNeeded = classesNeeded;
        }

        public Path getJarPath() {
            return jarPath;
        }

        public List<ClassDependency> getClassesNeeded() {
            return classesNeeded;
        }
    }

    public static class ClassDependency {
        private final String className;
        private final List<String> reasons;

        public ClassDependency(String className, List<String> reasons) {
            this.className = className;
            this.reasons = reasons;
        }

        public String getClassName() {
            return className;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }
}
