package dev.skidfuscator.pureanalysis.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.PurityContext;
import dev.skidfuscator.pureanalysis.PurityReport;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class DynamicInstructionAnalyzer extends InstructionAnalyzer {
    public DynamicInstructionAnalyzer(PurityContext context, PurityAnalyzer analyzer) {
        super("Indy", context, analyzer, InvokeDynamicInsnNode.class, LdcInsnNode.class);
    }

    @Override
    public PurityReport analyze(Context ctx) {
        final AbstractInsnNode insn = ctx.insn();

        if (insn instanceof InvokeDynamicInsnNode ||
            (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof ConstantDynamic)) {
            String owner = null;
            if (insn instanceof InvokeDynamicInsnNode) {
                Handle bootstrapHandle = ((InvokeDynamicInsnNode) insn).bsm;
                owner = bootstrapHandle.getOwner();
            }
            
            if (owner != null) {
                context.markImpure(owner);
            }
            return impure("Dynamic invocation", insn);
        }
        return pure();
    }
}