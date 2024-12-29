package dev.skidfuscator.pureanalysis.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.PurityContext;
import dev.skidfuscator.pureanalysis.PurityReport;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;

public class FieldInstructionAnalyzer extends InstructionAnalyzer {
    public FieldInstructionAnalyzer(PurityContext context, PurityAnalyzer analyzer) {
        super("Field", context, analyzer, FieldInsnNode.class);
    }

    @Override
    public PurityReport analyze(Context ctx) {
        FieldInsnNode fieldInsn = (FieldInsnNode) ctx.insn();

        if (fieldInsn.getOpcode() == Opcodes.PUTSTATIC) {
            // Mark the containing class as impure
            context.markImpure(fieldInsn.owner);
            return impure("Static field modification", fieldInsn);
        }

        if (fieldInsn.getOpcode() == Opcodes.GETSTATIC) {
            return impure(
                    "Static field read",
                    fieldInsn);
        }

        // Instance field operations are always pure unless the object is already impure
        if (!context.isPure(fieldInsn.owner)) {
            return impure("Field operation on impure object", fieldInsn);
        }

        return pure();
    }
}