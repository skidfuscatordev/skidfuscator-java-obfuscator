package dev.skidfuscator.pureanalysis.condition;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public abstract class PurityCondition {
    private List<PurityCondition> nestedConditions = new ArrayList<>();
    private final String name;

    public PurityCondition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addNestedCondition(PurityCondition condition) {
        nestedConditions.add(condition);
    }
    
    protected boolean evaluateNested(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        return nestedConditions.isEmpty() || 
               nestedConditions.stream()
                   .allMatch(c -> c.evaluateAndPrint(method, classNode, analyzer));
    }

    public boolean evaluateAndPrint(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        boolean output = evaluate(method, classNode, analyzer);

        if (!output) {
            System.out.println("Failed condition: " + getName() + " for " + classNode.name + "." + method.name + method.desc);
        }

        return output;
    }

    public abstract boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer);
}