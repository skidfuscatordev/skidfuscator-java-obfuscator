package dev.skidfuscator.obfuscator.exempt;

import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import java.util.*;

public class SimpleExemptAnalysis implements ExemptAnalysis {
    private final List<Exclusion> exclusions;
    private final Map<MethodNode, Boolean> methodCache = new HashMap<>();
    private final Map<ClassNode, Boolean> classCache = new HashMap<>();

    public SimpleExemptAnalysis(List<Exclusion> exclusionList) {
        this.exclusions = exclusionList;
    }

    public SimpleExemptAnalysis() {
        this(new ArrayList<>());
    }

    public void add(final String exclusionStr) {
        final Exclusion exclusion = ExclusionHelper.renderExclusion(exclusionStr);
        exclusions.add(exclusion);
    }

    @Override
    public boolean isExempt(MethodNode methodNode) {
        final Boolean var = methodCache.get(methodNode);

        if (var != null)
            return var;

        for (Exclusion exclusion : exclusions) {
            try {
                if (exclusion.test(methodNode)) {
                    methodCache.put(methodNode, true);
                    return true;
                }
            } catch (AssertionError e) {
                // Do nothing
            }
        }

        return false;
    }

    @Override
    public boolean isExempt(ClassNode classNode) {
        final Boolean var = classCache.get(classNode);

        if (var != null)
            return var;

        for (Exclusion exclusion : exclusions) {
            try {
                if (exclusion.test(classNode)) {
                    classCache.put(classNode, true);
                    return true;
                }
            } catch (AssertionError e) {
                // Do nothing
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return Arrays.toString(exclusions.toArray());
    }
}