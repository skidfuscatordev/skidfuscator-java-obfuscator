package dev.skidfuscator.pureanalysis.condition.impl.nested;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ObjectPurityCondition extends PurityCondition {
    private final Set<String> knownPureClasses = new HashSet<>(Arrays.asList(
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
    ));

    public ObjectPurityCondition() {
        super("Nested - Object purity");
    }

    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        ObjectStateTracker tracker = new ObjectStateTracker(analyzer, knownPureClasses);
        return tracker.analyze(method, classNode) && evaluateNested(method, classNode, analyzer);
    }

    private static class ObjectStateTracker {
        private final PurityAnalyzer analyzer;
        private final Set<String> pureClasses;
        private final Set<Integer> trackedLocals = new HashSet<>();
        private final Set<String> trackedMethodSignatures = new HashSet<>();

        public ObjectStateTracker(PurityAnalyzer analyzer, Set<String> pureClasses) {
            this.analyzer = analyzer;
            this.pureClasses = pureClasses;
        }

        public boolean analyze(MethodNode method, ClassNode classNode) {
            // Reset state
            trackedLocals.clear();
            trackedMethodSignatures.clear();

            // Track object creations and method calls
            for (AbstractInsnNode insn : method.instructions) {
                if (!analyzeInstruction(insn, method, classNode)) {
                    return false;
                }
            }
            return true;
        }

        private boolean analyzeInstruction(AbstractInsnNode insn, MethodNode method, ClassNode classNode) {
            switch (insn.getType()) {
                case AbstractInsnNode.TYPE_INSN:
                    return analyzeTypeInstruction((TypeInsnNode) insn);
                case AbstractInsnNode.METHOD_INSN:
                    return analyzeMethodInstruction((MethodInsnNode) insn, classNode);
                case AbstractInsnNode.FIELD_INSN:
                    return analyzeFieldInstruction((FieldInsnNode) insn);
                case AbstractInsnNode.VAR_INSN:
                    return analyzeVarInstruction((VarInsnNode) insn);
                default:
                    return true;
            }
        }

        private boolean analyzeTypeInstruction(TypeInsnNode insn) {
            if (insn.getOpcode() == Opcodes.NEW) {
                // Track newly created objects
                return isPureType(insn.desc) || isLocallyCreatedType(insn.desc);
            }
            return true;
        }

        private boolean analyzeMethodInstruction(MethodInsnNode insn, ClassNode currentClass) {
            String signature = insn.owner + "." + insn.name + insn.desc;

            // Handle constructor calls
            if (insn.name.equals("<init>")) {
                return isPureType(insn.owner) || isLocallyCreatedType(insn.owner);
            }

            // Already verified this method call
            if (trackedMethodSignatures.contains(signature)) {
                return true;
            }
            trackedMethodSignatures.add(signature);

            // Handle method calls on pure objects
            if (isPureType(insn.owner)) {
                return true;
            }

            // Handle virtual method calls
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL || 
                insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                return analyzePureVirtualCall(insn);
            }

            return true;
        }

        private boolean analyzeFieldInstruction(FieldInsnNode insn) {
            // Allow field access on pure objects
            if (isPureType(insn.owner)) {
                return true;
            }

            // Allow field modifications only on locally created objects
            if (insn.getOpcode() == Opcodes.PUTFIELD) {
                return isLocallyCreatedType(insn.owner);
            }

            return true;
        }

        private boolean analyzeVarInstruction(VarInsnNode insn) {
            // Track local variable usage
            if (insn.getOpcode() >= Opcodes.ISTORE && insn.getOpcode() <= Opcodes.ASTORE) {
                trackedLocals.add(insn.var);
            }
            return true;
        }

        private boolean analyzePureVirtualCall(MethodInsnNode methodInsn) {
            try {
                ClassNode targetClass = analyzer.getHierarchyAnalyzer().getClass(methodInsn.owner);
                
                // Check if the class is effectively immutable
                if (isEffectivelyImmutable(targetClass)) {
                    return true;
                }

                // Check if the method only modifies its own state
                for (MethodNode method : targetClass.methods) {
                    if (method.name.equals(methodInsn.name) && method.desc.equals(methodInsn.desc)) {
                        return analyzeMethodPurity(method, targetClass);
                    }
                }
            } catch (Exception e) {
                // If we can't analyze, be conservative
                return false;
            }
            return false;
        }

        private boolean isEffectivelyImmutable(ClassNode classNode) {
            // Check if all fields are final
            boolean allFieldsFinal = classNode.fields.stream()
                .allMatch(field -> (field.access & Opcodes.ACC_FINAL) != 0);

            // Check if class is final
            boolean isFinal = (classNode.access & Opcodes.ACC_FINAL) != 0;

            return allFieldsFinal && isFinal;
        }

        private boolean analyzeMethodPurity(MethodNode method, ClassNode classNode) {
            // Check if method only modifies its own fields
            ObjectStateTracker newTracker = new ObjectStateTracker(analyzer, pureClasses);
            return newTracker.analyze(method, classNode);
        }

        private boolean isPureType(String type) {
            return pureClasses.contains(type);
        }

        private boolean isLocallyCreatedType(String type) {
            try {
                ClassNode classNode = analyzer.getHierarchyAnalyzer().getClass(type);
                // Check if this type only modifies its own state
                return isEffectivelyImmutable(classNode) || 
                       isLocalStateOnly(classNode);
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isLocalStateOnly(ClassNode classNode) {
            // Check if all methods only modify their own object's state
            return classNode.methods.stream()
                .filter(m -> !m.name.equals("<init>"))
                .allMatch(m -> {
                    ObjectStateTracker methodTracker = new ObjectStateTracker(analyzer, pureClasses);
                    return methodTracker.analyze(m, classNode);
                });
        }
    }
}