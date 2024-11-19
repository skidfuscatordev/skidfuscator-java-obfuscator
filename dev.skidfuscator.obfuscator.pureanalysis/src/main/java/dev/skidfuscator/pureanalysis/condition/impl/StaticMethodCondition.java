package dev.skidfuscator.pureanalysis.condition.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class StaticMethodCondition extends PurityCondition {
    public StaticMethodCondition() {
        super("Static method");
    }

    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        // Check if the ACC_STATIC flag is set in the method's access flags
        boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        return isStatic && evaluateNested(method, classNode, analyzer);
    }
}