package dev.skidfuscator.pureanalysis;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClassHierarchyAnalyzer {
    private final ConcurrentHashMap<String, ClassNode> classCache = new ConcurrentHashMap<>();
    private final ClassLoader classLoader;
    private final Set<String> analyzedClasses = ConcurrentHashMap.newKeySet();
    
    public ClassHierarchyAnalyzer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    public ClassNode getClass(String className) throws IOException {
        return classCache.computeIfAbsent(className, this::loadClass);
    }
    
    private ClassNode loadClass(String className) {
        try {
            ClassReader reader = new ClassReader(classLoader.getResourceAsStream(
                className.replace('.', '/') + ".class"));
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return classNode;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load class: " + className, e);
        }
    }
    
    public Set<String> getAllSuperClasses(String className) throws IOException {
        Set<String> superClasses = new HashSet<>();
        collectSuperClasses(className, superClasses);
        return superClasses;
    }
    
    private void collectSuperClasses(String className, Set<String> collected) throws IOException {
        if (className == null || className.equals("java/lang/Object")) return;
        
        ClassNode classNode = getClass(className);
        collected.add(className);
        
        // Collect superclass
        if (classNode.superName != null) {
            collectSuperClasses(classNode.superName, collected);
        }
        
        // Collect interfaces
        for (String interfaceName : classNode.interfaces) {
            collectSuperClasses(interfaceName, collected);
        }
    }
}