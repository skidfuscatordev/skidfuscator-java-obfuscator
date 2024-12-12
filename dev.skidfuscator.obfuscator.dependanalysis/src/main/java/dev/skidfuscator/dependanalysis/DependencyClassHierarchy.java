package dev.skidfuscator.dependanalysis;

import java.nio.file.Path;

/**
 * A simple data structure holding the hierarchy information of a single class:
 *  - The class name
 *  - Its immediate superclass name
 *  - Any interfaces it implements
 *  - Whether it was found in the main jar or a library jar
 *  - The source jar path (if from a library)
 */
public class DependencyClassHierarchy {
    public final String className;
    public final String superName;
    public final String[] interfaces;
    public final boolean isMainJarClass;
    public final Path sourceJar;

    public DependencyClassHierarchy(String className, String superName, String[] interfaces, boolean isMainJarClass, Path sourceJar) {
        this.className = className;
        this.superName = superName;
        this.interfaces = interfaces;
        this.isMainJarClass = isMainJarClass;
        this.sourceJar = sourceJar;
    }
}
