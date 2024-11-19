package dev.skidfuscator.pureanalysis.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.PurityContext;
import dev.skidfuscator.pureanalysis.PurityReport;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodInstructionAnalyzer extends InstructionAnalyzer {
    public MethodInstructionAnalyzer(PurityContext context, PurityAnalyzer analyzer) {
        super("Method Analyzer", context, analyzer, MethodInsnNode.class);
    }

    @Override
    public PurityReport analyze(Context ctx) {
        MethodInsnNode methodInsn = (MethodInsnNode) ctx.insn();

        // Only check if the object has been marked impure
        if (!context.isPure(methodInsn.owner)) {
            return impure("Method call on impure object", methodInsn);
        }

        return analyzeMethodCall(methodInsn);
    }

    private PurityReport analyzeMethodCall(MethodInsnNode insn) {
        String signature = insn.owner + "." + insn.name + insn.desc;

        boolean isStatic = insn.getOpcode() == Opcodes.INVOKESTATIC;
        if (isStatic ? context.isPureStaticMethod(signature) : context.isPureMethods(signature)) {
            return pure();
        }

        try {
            ClassNode targetClass = analyzer.getHierarchyAnalyzer().getClass(insn.owner);
            for (MethodNode targetMethod : targetClass.methods) {
                if (targetMethod.name.equals(insn.name) && targetMethod.desc.equals(insn.desc)) {
                    PurityReport targetReport = analyzer.analyzeMethodPurity(targetMethod, targetClass);
                    if (!targetReport.isPure()) {
                        return impure("Method call to impure method: " + signature, insn);
                    }

                    if (isStatic) {
                        context.addPureStaticMethod(signature);
                    } else {
                        context.addPureMethods(signature);
                    }

                    return pure();
                }
            }
        } catch (Exception e) {
            return impure("Unable to analyze method: " + signature, insn);
        }

        return impure("Unknown static method: " + signature, insn);
    }
}