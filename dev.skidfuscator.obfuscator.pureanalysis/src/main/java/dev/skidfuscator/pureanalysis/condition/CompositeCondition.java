package dev.skidfuscator.pureanalysis.condition;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class CompositeCondition extends PurityCondition {
    public enum Operation { AND, OR }
    
    private final List<PurityCondition> conditions = new ArrayList<>();
    private final Operation operation;
    
    public CompositeCondition(Operation operation) {
        super("Composite");
        this.operation = operation;
    }
    
    public void addCondition(PurityCondition condition) {
        conditions.add(condition);
    }
    
    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        if (conditions.isEmpty()) return true;
        
        boolean result = operation == Operation.AND;
        for (PurityCondition condition : conditions) {
            if (operation == Operation.AND) {
                result &= condition.evaluateAndPrint(method, classNode, analyzer);
                if (!result) break; // Short circuit AND
            } else {
                result |= condition.evaluateAndPrint(method, classNode, analyzer);
                if (result) break;  // Short circuit OR
            }
        }
        return result && evaluateNested(method, classNode, analyzer);
    }

    @Override
    public String getName() {
        return "Composite: \n" + conditions.stream().map(PurityCondition::getName).reduce((a, b) -> a + "\n" + b).orElse("");
    }
}
