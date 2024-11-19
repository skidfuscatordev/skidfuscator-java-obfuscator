package dev.skidfuscator.pureanalysis.condition.impl.nested;


import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NoSideEffectsCondition extends PurityCondition {

    public NoSideEffectsCondition() {
        super("Nested - Parent");
    }

    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        PurityContext context = new PurityContext(analyzer);
        ContextualAnalyzer contextualAnalyzer = new ContextualAnalyzer(context);
        return contextualAnalyzer.analyze(method, classNode) && 
               evaluateNested(method, classNode, analyzer);
    }

    private static class ContextualAnalyzer {
        private final PurityContext context;

        public ContextualAnalyzer(PurityContext context) {
            this.context = context;
            initializeKnownPureTypes();
        }

        private void initializeKnownPureTypes() {
            Arrays.asList(
                "java/lang/String",
                "java/lang/Integer",
                "java/lang/Long",
                "java/lang/Double",
                "java/lang/Float",
                "java/lang/Boolean",
                "java/lang/Character",
                "java/math/BigInteger",
                "java/math/BigDecimal",
                "java/time/LocalDate",
                "java/time/LocalDateTime",
                "java/time/Instant"
            ).forEach(context::registerPureObject);
        }

        public boolean analyze(MethodNode method, ClassNode classNode) {
            InstructionAnalyzer analyzer = new InstructionAnalyzer(method, classNode, context);
            return analyzer.analyzeInstructions();
        }
    }

    private static class InstructionAnalyzer {
        private final MethodNode method;
        private final ClassNode classNode;
        private final PurityContext context;
        private final Map<Integer, String> varTypeMap = new HashMap<>();

        public InstructionAnalyzer(MethodNode method, ClassNode classNode, PurityContext context) {
            this.method = method;
            this.classNode = classNode;
            this.context = context;
        }

        public boolean analyzeInstructions() {
            for (AbstractInsnNode insn : method.instructions) {
                if (!analyzeInstruction(insn)) {
                    return false;
                }
            }
            return true;
        }

        private boolean analyzeInstruction(AbstractInsnNode insn) {
            switch (insn.getType()) {
                case AbstractInsnNode.TYPE_INSN:
                    return analyzeTypeInstruction((TypeInsnNode) insn);
                case AbstractInsnNode.METHOD_INSN:
                    return analyzeMethodInstruction((MethodInsnNode) insn);
                case AbstractInsnNode.FIELD_INSN:
                    return analyzeFieldInstruction((FieldInsnNode) insn);
                case AbstractInsnNode.VAR_INSN:
                    return analyzeVarInstruction((VarInsnNode) insn);
                case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                    return false; // We consider invoke dynamic impure for now
                default:
                    return true;
            }
        }

        private boolean analyzeTypeInstruction(TypeInsnNode insn) {
            if (insn.getOpcode() == Opcodes.NEW) {
                // If we're creating a new object, consider it pure until proven otherwise
                context.registerPureObject(insn.desc);
                return true;
            }
            return true;
        }

        private boolean analyzeMethodInstruction(MethodInsnNode insn) {
            String signature = insn.owner + "." + insn.name + insn.desc;

            // Constructor calls are fine for pure objects
            if (insn.name.equals("<init>") && context.isPureObject(insn.owner)) {
                return true;
            }

            // If the object is pure, allow method calls on it
            if (context.isPureObject(insn.owner)) {
                return true;
            }

            // For virtual calls, check object purity
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL || 
                insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                return analyzeVirtualCall(insn);
            }

            // For static calls, analyze the method
            return context.getAnalyzer().isPureMethod(insn.owner, insn.name, insn.desc);
        }

        private boolean analyzeFieldInstruction(FieldInsnNode fieldInsn) {
            // Reading is always allowed
            if (fieldInsn.getOpcode() == Opcodes.GETFIELD || 
                fieldInsn.getOpcode() == Opcodes.GETSTATIC) {
                return true;
            }

            // Allow modifications to pure objects
            if (context.isPureObject(fieldInsn.owner)) {
                return true;
            }

            // Static field writes are never pure
            if (fieldInsn.getOpcode() == Opcodes.PUTSTATIC) {
                return false;
            }

            // Instance field writes are allowed on the object's own fields
            return fieldInsn.owner.equals(classNode.name);
        }

        private boolean analyzeVarInstruction(VarInsnNode insn) {
            context.addLocalVar(insn.var);
            return true;
        }

        private boolean analyzeVirtualCall(MethodInsnNode methodInsn) {
            try {
                ClassNode targetClass = context.getAnalyzer()
                    .getHierarchyAnalyzer()
                    .getClass(methodInsn.owner);

                // If the class is final and pure, allow the call
                if ((targetClass.access & Opcodes.ACC_FINAL) != 0 && 
                    context.isPureObject(methodInsn.owner)) {
                    return true;
                }

                // For non-final classes, check if the method only modifies its own state
                return targetClass.methods.stream()
                    .filter(m -> m.name.equals(methodInsn.name) && 
                               m.desc.equals(methodInsn.desc))
                    .findFirst()
                    .map(m -> context.getAnalyzer().analyzeMethod(m, targetClass))
                    .orElse(false);
            } catch (Exception e) {
                return false;
            }
        }
    }
}