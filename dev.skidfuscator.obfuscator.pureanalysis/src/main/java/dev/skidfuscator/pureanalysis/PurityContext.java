package dev.skidfuscator.pureanalysis;

import java.util.*;

import org.objectweb.asm.tree.ClassNode;

public class PurityContext {
    private final Set<String> impureObjects = new HashSet<>();
    private final Map<String, Purity> pureStaticMethods = new HashMap<>();
    private final Map<String, Purity> pureMethods = new HashMap<>();
    private final Map<String, Set<String>> hierarchyCache = new HashMap<>();

    private final PurityAnalyzer analyzer;

    public PurityContext(PurityAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    private Set<String> computeHierarchy(String type) {
        try {
            Set<String> hierarchy = new HashSet<>();
            String current = type;
            
            while (current != null && !current.equals("java/lang/Object")) {
                hierarchy.add(current);
                ClassNode classNode = analyzer.getHierarchyAnalyzer().getClass(current);
                current = classNode.superName;
                
                // Add interfaces
                for (String iface : classNode.interfaces) {
                    hierarchy.add(iface);
                    // Recursively add interface hierarchies
                    hierarchy.addAll(getHierarchy(iface));
                }
            }
            
            return hierarchy;
        } catch (Exception e) {
            return Collections.singleton(type);
        }
    }

    public Set<String> getHierarchy(String type) {
        return hierarchyCache.computeIfAbsent(type, this::computeHierarchy);
    }

    public void markImpure(String type) {
        impureObjects.add(type);
    }

    public boolean isPure(String type) {
        return !impureObjects.contains(type);
    }

    public boolean isPureStaticMethod(String signature) {
        return pureStaticMethods.getOrDefault(signature, Purity.MUD).isPure();
    }

    public void addPureStaticMethod(String signature) {
        pureStaticMethods.put(signature, Purity.PURE);
    }

    public boolean isPureMethods(String signature) {
        return pureMethods.getOrDefault(signature, Purity.MUD).isPure();
    }

    public void addPureMethods(String signature) {
        pureMethods.put(signature, Purity.PURE);
    }
}