package dev.skidfuscator.obfuscator.exempt;

import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import java.util.*;

public class SimpleExemptAnalysis implements ExemptAnalysis {
    private final Class<? extends Transformer> transformer;
    private final List<Exclusion> exclusions;
    private final Set<ClassNode> explicitClasses = new LinkedHashSet<>();
    private final Set<MethodNode> explicitMethods = new LinkedHashSet<>();
    private final Map<MethodNode, Boolean> methodCache = new HashMap<>();
    private final Map<ClassNode, Boolean> classCache = new HashMap<>();

    public SimpleExemptAnalysis(Class<? extends Transformer> transformer) {
        this(transformer, new ArrayList<>());
    }

    public SimpleExemptAnalysis(Class<? extends Transformer> transformer, List<Exclusion> exclusions) {
        this.transformer = transformer;
        this.exclusions = exclusions;
    }

    public void add(final String exclusionStr) {
        final Exclusion exclusion = ExclusionHelper.renderExclusion(exclusionStr);
        exclusions.add(exclusion);
        methodCache.clear(); classCache.clear();

        //System.out.println(this);
    }

    public void add(final Exclusion exclusion) {
        exclusions.add(exclusion);
        methodCache.clear(); classCache.clear();
    }

    @Override
    public void add(ClassNode exclusion) {
        explicitClasses.add(exclusion);
        classCache.put(exclusion, true);
    }

    @Override
    public void add(MethodNode exclusion) {
        explicitMethods.add(exclusion);
        methodCache.put(exclusion, true);
    }

    @Override
    public boolean isExempt(MethodNode methodNode) {
        if (explicitMethods.contains(methodNode)) return true;
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
                e.printStackTrace();
            }
        }

        methodCache.put(methodNode, false);

        return false;
    }

    @Override
    public boolean isExempt(ClassNode classNode) {
        if (explicitClasses.contains(classNode)) return true;
        final Boolean var = classCache.get(classNode);

        if (var != null)
            return var;

        for (Exclusion exclusion : exclusions) {
            //System.out.println("Testing " + exclusion);
            try {
                if (exclusion.test(classNode)) {
                    classCache.put(classNode, true);
                    //System.out.println("EXCLUDED --> " + classNode.getName());
                    return true;
                }
            } catch (AssertionError e) {
                // Do nothing
                e.printStackTrace();
            }
        }
        //System.out.println("INCLUDED --> " + classNode.getName());
        classCache.put(classNode, false);
        return false;
    }

    public boolean isExempt(org.mapleir.asm.FieldNode field) {
        return isExempt(field.getOwnerClass()) || exclusions.stream().anyMatch(rule -> rule.test(field));
    }

    @Override
    public String toString() {
        return Arrays.toString(exclusions.toArray());
    }
}