package dev.skidfuscator.pureanalysis;

import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PurityAnalyzer {
    private final ConcurrentHashMap<String, Boolean> pureClasses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> methodPurityCache = new ConcurrentHashMap<>();
    private final ClassHierarchyAnalyzer hierarchyAnalyzer;
    private final List<PurityCondition> conditions;

    // ThreadLocal set to track methods being analyzed in the current thread
    private final ThreadLocal<Set<String>> methodsUnderAnalysis = ThreadLocal.withInitial(HashSet::new);

    public PurityAnalyzer(ClassLoader classLoader) {
        this.hierarchyAnalyzer = new ClassHierarchyAnalyzer(classLoader);
        this.conditions = new ArrayList<>();
    }

    public void addCondition(PurityCondition condition) {
        conditions.add(condition);
    }

    public void registerPureClass(String className) {
        pureClasses.put(className, true);
    }

    public boolean isPureClass(String className) {
        return pureClasses.getOrDefault(className, false);
    }

    public boolean isPureMethod(String owner, String name, String desc) {
        String key = owner + "." + name + desc;

        // If the method is currently being analyzed, assume it's pure to break recursion
        if (methodsUnderAnalysis.get().contains(key)) {
            return true;
        }

        return methodPurityCache.getOrDefault(key, false);
    }

    public boolean analyzeMethod(MethodNode method, ClassNode classNode) {
        String methodKey = classNode.name + "." + method.name + method.desc;

        // If the method is already cached, return the cached result
        Boolean cachedResult = methodPurityCache.get(methodKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // If we're already analyzing this method, return true to break recursion
        Set<String> currentMethods = methodsUnderAnalysis.get();
        if (currentMethods.contains(methodKey)) {
            return true;
        }

        // Add this method to the set of methods being analyzed
        currentMethods.add(methodKey);

        try {
            // Evaluate all conditions
            boolean isPure = true;
            for (PurityCondition condition : conditions) {
                boolean result = condition.evaluateAndPrint(method, classNode, this);
                if (!result) {
                    isPure = false;
                    break;
                }
            }

            // Cache the result
            methodPurityCache.put(methodKey, isPure);
            return isPure;
        } finally {
            // Remove this method from the set of methods being analyzed
            currentMethods.remove(methodKey);
            if (currentMethods.isEmpty()) {
                methodsUnderAnalysis.remove();
            }
        }
    }

    public ClassHierarchyAnalyzer getHierarchyAnalyzer() {
        return hierarchyAnalyzer;
    }

    private final Set<String> analyzedClasses = ConcurrentHashMap.newKeySet();

    public void analyzeClass(String className) throws IOException {
        if (analyzedClasses.contains(className)) {
            return;
        }

        ClassNode classNode = hierarchyAnalyzer.getClass(className);

        // Analyze all methods in the class
        for (MethodNode method : classNode.methods) {
            analyzeMethod(method, classNode);
        }

        analyzedClasses.add(className);
    }
}