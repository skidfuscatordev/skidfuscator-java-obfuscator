package dev.skidfuscator.dependanalysis;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * A simple entry point for demonstration purposes. Provide:
 *  java com.example.dependencyanalyzer.Main <main.jar> <lib_folder>
 *
 * For instance:
 *  java com.example.dependencyanalyzer.Main my-app.jar libs/
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java com.example.dependencyanalyzer.Main <main.jar> <lib_folder>");
            System.exit(1);
        }

        Path mainJar = Paths.get(args[0]);
        Path libs = Paths.get(args[1]);

        DependencyAnalyzer analyzer = new DependencyAnalyzer(mainJar, libs);
        DependencyResult requiredJars = analyzer.analyze();

        System.out.println("Required jars:");
        for (DependencyResult.JarDependency jar : requiredJars.getJarDependencies()) {
            System.out.println(" - " + jar);
        }
    }
}
