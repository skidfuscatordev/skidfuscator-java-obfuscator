package dev.skidfuscator.pureanalysis.condition.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import dev.skidfuscator.pureanalysis.condition.impl.nested.FieldAccessCondition;
import dev.skidfuscator.pureanalysis.condition.impl.nested.InvokeDynamicCondition;
import dev.skidfuscator.pureanalysis.condition.impl.nested.ObjectPurityCondition;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

public class NoSideEffectsCondition8Old extends PurityCondition {
    private final Set<PurityCondition> conditions = new HashSet<>();
    private final MethodCallTracker methodCallTracker;

    public NoSideEffectsCondition8Old(PurityAnalyzer analyzer) {
        super("No side effects");
        this.methodCallTracker = new MethodCallTracker(analyzer);
        setupConditions();
    }

    private void setupConditions() {
        conditions.add(new FieldAccessCondition());
        conditions.add(new InvokeDynamicCondition());
        conditions.add(methodCallTracker);
        conditions.add(new ObjectPurityCondition());
    }

    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        for (PurityCondition condition : conditions) {
            if (!condition.evaluate(method, classNode, analyzer)) {
                return false;
            }
        }
        return evaluateNested(method, classNode, analyzer);
    }

    private static class MethodCallTracker extends PurityCondition {
        private final PurityAnalyzer analyzer;
        private final ThreadLocal<Set<String>> callStack = ThreadLocal.withInitial(HashSet::new);

        public MethodCallTracker(PurityAnalyzer analyzer) {
            super("Method call");
            this.analyzer = analyzer;
        }

        @Override
        public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
            String methodKey = classNode.name + "." + method.name + method.desc;
            Set<String> currentCallStack = callStack.get();

            if (currentCallStack.contains(methodKey)) {
                return true; // Handle recursion
            }

            currentCallStack.add(methodKey);
            try {
                return analyzeMethodCalls(method, classNode);
            } finally {
                currentCallStack.remove(methodKey);
                if (currentCallStack.isEmpty()) {
                    callStack.remove();
                }
            }
        }

        private boolean analyzeMethodCalls(MethodNode method, ClassNode classNode) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (!isMethodCallPure(methodInsn, classNode)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isMethodCallPure(MethodInsnNode methodInsn, ClassNode currentClass) {
            // Handle virtual/interface calls
            if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL ||
                    methodInsn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                return isPureVirtualCall(methodInsn);
            }

            // Handle static/special calls
            return analyzer.isPureMethod(methodInsn.owner, methodInsn.name, methodInsn.desc) ||
                    analyzeTargetMethod(methodInsn);
        }

        private boolean isPureVirtualCall(MethodInsnNode methodInsn) {
            try {
                ClassNode targetClass = analyzer.getHierarchyAnalyzer().getClass(methodInsn.owner);
                // Check if the class is effectively final
                boolean isFinal = (targetClass.access & Opcodes.ACC_FINAL) != 0;

                // For final classes, we can analyze the method directly
                if (isFinal) {
                    return analyzeTargetMethod(methodInsn);
                }

                // For non-final classes, be conservative
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean analyzeTargetMethod(MethodInsnNode methodInsn) {
            try {
                ClassNode targetClass = analyzer.getHierarchyAnalyzer().getClass(methodInsn.owner);
                for (MethodNode targetMethod : targetClass.methods) {
                    if (targetMethod.name.equals(methodInsn.name) &&
                            targetMethod.desc.equals(methodInsn.desc)) {
                        return analyzer.analyzeMethod(targetMethod, targetClass);
                    }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }
}