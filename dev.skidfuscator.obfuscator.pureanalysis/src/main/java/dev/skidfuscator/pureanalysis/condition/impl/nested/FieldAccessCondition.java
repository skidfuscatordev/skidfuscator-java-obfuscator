package dev.skidfuscator.pureanalysis.condition.impl.nested;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class FieldAccessCondition extends PurityCondition {
    public FieldAccessCondition() {
        super("Nested - Field");
    }

    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (!isValidFieldAccess(fieldInsn, classNode, analyzer)) {
                    return false;
                }
            }
        }
        return evaluateNested(method, classNode, analyzer);
    }

    private boolean isValidFieldAccess(FieldInsnNode fieldInsn, ClassNode classNode, PurityAnalyzer analyzer) {
        switch (fieldInsn.getOpcode()) {
            case Opcodes.GETSTATIC:
                return isFinalField(fieldInsn, analyzer);
            case Opcodes.PUTSTATIC:
                return false;
            case Opcodes.GETFIELD:
                return true;
            case Opcodes.PUTFIELD:
                return fieldInsn.owner.equals(classNode.name);
            default:
                return false;
        }
    }

    private boolean isFinalField(FieldInsnNode fieldInsn, PurityAnalyzer analyzer) {
        try {
            ClassNode targetClass = analyzer.getHierarchyAnalyzer().getClass(fieldInsn.owner);
            return targetClass.fields.stream()
                .filter(f -> f.name.equals(fieldInsn.name))
                .anyMatch(f -> (f.access & Opcodes.ACC_FINAL) != 0);
        } catch (Exception e) {
            return false;
        }
    }
}