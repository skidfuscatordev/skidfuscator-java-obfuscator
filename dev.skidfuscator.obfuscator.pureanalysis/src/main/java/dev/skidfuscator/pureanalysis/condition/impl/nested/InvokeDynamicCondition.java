package dev.skidfuscator.pureanalysis.condition.impl.nested;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class InvokeDynamicCondition extends PurityCondition {
    public InvokeDynamicCondition() {
        super("Nested - Indy");
    }

    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof InvokeDynamicInsnNode) {
                return false;
            }
        }
        return evaluateNested(method, classNode, analyzer);
    }
}
