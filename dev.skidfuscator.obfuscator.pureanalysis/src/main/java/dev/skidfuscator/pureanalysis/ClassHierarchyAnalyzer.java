package dev.skidfuscator.pureanalysis;

import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Set;

public interface ClassHierarchyAnalyzer {
    ClassNode getClass(String className) throws IOException;

    Set<String> getAllSuperClasses(String className) throws IOException;
}
