package dev.skidfuscator.obfuscator.exempt;

import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import java.util.HashMap;
import java.util.Map;

public class ExemptManager {
    private final SimpleExemptAnalysis globalExempt = new SimpleExemptAnalysis(null);
    private final Map<Class<? extends Transformer>, ExemptAnalysis> exemptAnalysisMap;

    public ExemptManager() {
        exemptAnalysisMap = new HashMap<>();
    }

    public void add(String exempt) {
        globalExempt.add(exempt);
    }

    public void add(ClassNode exclusion) {
        globalExempt.add(exclusion);
    }

    public void add(MethodNode exclusion) {
        globalExempt.add(exclusion);
    }

    public boolean isExempt(final ClassNode classNode) {
        return globalExempt.isExempt(classNode);
    }

    public boolean isExempt(final MethodNode methodNode) {
        if (methodNode.getName().equals("clone") && methodNode.isBridge())
            return true;

        return globalExempt.isExempt(methodNode);
    }

    public void add(final Class<? extends Transformer> clazz, String exempt) {
        exemptAnalysisMap
                .computeIfAbsent(clazz, e -> new SimpleExemptAnalysis(clazz))
                .add(exempt);
    }

    public boolean isExempt(final Class<? extends Transformer> clazz, ClassNode classNode) {
        return exemptAnalysisMap
                .computeIfAbsent(clazz, e -> new SimpleExemptAnalysis(clazz))
                .isExempt(classNode);
    }

    public boolean isExempt(final Class<? extends Transformer> clazz, MethodNode methodNode) {
        return exemptAnalysisMap
                .computeIfAbsent(clazz, e -> new SimpleExemptAnalysis(clazz))
                .isExempt(methodNode);
    }
}
