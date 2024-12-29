package framework;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

class MethodTestInfo {
    final String className;
    final String methodName;
    final String descriptor;
    final boolean expectedPure;
    final String description;
    final String[] reasons;
    final ClassNode classNode;
    final MethodNode methodNode;

    MethodTestInfo(String className, String methodName, String descriptor, 
                  boolean expectedPure, String description, String[] reasons,
                  ClassNode classNode, MethodNode methodNode) {
        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.expectedPure = expectedPure;
        this.description = description;
        this.reasons = reasons;
        this.classNode = classNode;
        this.methodNode = methodNode;
    }

    String getDisplayName() {
        return String.format("%s#%s%s [%s]", 
            className.substring(className.lastIndexOf('.') + 1),
            methodName, 
            descriptor,
            expectedPure ? "pure" : "impure"
        );
    }
}