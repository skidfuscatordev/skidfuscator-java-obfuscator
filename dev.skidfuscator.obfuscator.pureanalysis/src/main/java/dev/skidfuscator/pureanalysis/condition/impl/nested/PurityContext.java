package dev.skidfuscator.pureanalysis.condition.impl.nested;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;

import java.util.HashSet;
import java.util.Set;

class PurityContext {
    private final Set<String> pureObjects = new HashSet<>();
    private final Set<Integer> localVars = new HashSet<>();
    private final Set<String> analyzedMethods = new HashSet<>();
    private final PurityAnalyzer analyzer;

    public PurityContext(PurityAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void registerPureObject(String type) {
        pureObjects.add(type);
    }

    public boolean isPureObject(String type) {
        return pureObjects.contains(type);
    }

    public void addLocalVar(int var) {
        localVars.add(var);
    }

    public boolean isLocalVar(int var) {
        return localVars.contains(var);
    }

    public PurityAnalyzer getAnalyzer() {
        return analyzer;
    }

    public boolean isMethodAnalyzed(String signature) {
        return analyzedMethods.contains(signature);
    }

    public void markMethodAnalyzed(String signature) {
        analyzedMethods.add(signature);
    }
}