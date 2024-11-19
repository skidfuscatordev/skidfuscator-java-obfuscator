package dev.skidfuscator.pureanalysis.condition.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ArrayStateCondition extends PurityCondition {
    private final Set<Integer> localArrayVars = new HashSet<>();
    private final Set<Integer> parameterArrayVars = new HashSet<>();
    
    public ArrayStateCondition() {
        super("Array state tracking");
    }

    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        // Reset state
        localArrayVars.clear();
        parameterArrayVars.clear();
        
        // Initialize parameter array variables
        Type[] args = Type.getArgumentTypes(method.desc);
        int varIndex = (method.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (Type arg : args) {
            if (arg.getSort() == Type.ARRAY) {
                parameterArrayVars.add(varIndex);
            }
            varIndex += arg.getSize();
        }

        ArrayUsageAnalyzer arrayAnalyzer = new ArrayUsageAnalyzer();
        return arrayAnalyzer.analyze(method) && evaluateNested(method, classNode, analyzer);
    }

    private class ArrayUsageAnalyzer {
        private final Set<Integer> safeArrays = new HashSet<>();

        public boolean analyze(MethodNode method) {
            for (AbstractInsnNode insn : method.instructions) {
                if (!analyzeInstruction(insn, method)) {
                    return false;
                }
            }
            return true;
        }

        private boolean analyzeInstruction(AbstractInsnNode insn, MethodNode method) {
            switch (insn.getType()) {
                case AbstractInsnNode.INSN:
                    return analyzeBasicInstruction(insn);
                case AbstractInsnNode.VAR_INSN:
                    return analyzeVarInstruction((VarInsnNode) insn);
                case AbstractInsnNode.TYPE_INSN:
                    return analyzeTypeInstruction((TypeInsnNode) insn);
                case AbstractInsnNode.MULTIANEWARRAY_INSN:
                    handleNewArray();
                    return true;
                case AbstractInsnNode.INT_INSN:
                    return analyzeIntInstruction((IntInsnNode) insn);
                default:
                    return true;
            }
        }

        private boolean analyzeBasicInstruction(AbstractInsnNode insn) {
            int opcode = insn.getOpcode();
            
            // Check for array store operations
            if (opcode >= Opcodes.IASTORE && opcode <= Opcodes.SASTORE) {
                // Get the array reference from the stack simulation
                return isLocalArrayModification();
            }
            
            // Array creation operations
            if (opcode == Opcodes.NEWARRAY) {
                handleNewArray();
            }
            
            return true;
        }

        private boolean analyzeVarInstruction(VarInsnNode insn) {
            // Track array loads and stores to local variables
            int var = insn.var;
            
            if (insn.getOpcode() == Opcodes.ASTORE) {
                if (isArrayOnStack()) {
                    if (isNewArrayOnStack()) {
                        localArrayVars.add(var);
                    } else {
                        // This might be storing a parameter array
                        parameterArrayVars.add(var);
                    }
                }
            }
            
            return true;
        }

        private boolean analyzeTypeInstruction(TypeInsnNode insn) {
            if (insn.getOpcode() == Opcodes.ANEWARRAY) {
                handleNewArray();
            }
            return true;
        }

        private boolean analyzeIntInstruction(IntInsnNode insn) {
            if (insn.getOpcode() == Opcodes.NEWARRAY) {
                handleNewArray();
            }
            return true;
        }

        private void handleNewArray() {
            safeArrays.add(getCurrentStackDepth());
        }

        private boolean isLocalArrayModification() {
            // This is a simplified check - in a real implementation,
            // you'd need proper stack frame analysis
            return !parameterArrayVars.contains(getArrayReferenceFromStack());
        }

        private boolean isArrayOnStack() {
            // In a real implementation, you'd need proper stack frame analysis
            return true;
        }

        private boolean isNewArrayOnStack() {
            return safeArrays.contains(getCurrentStackDepth());
        }

        private int getCurrentStackDepth() {
            // This would need proper stack frame analysis in a real implementation
            return 0;
        }

        private int getArrayReferenceFromStack() {
            // This would need proper stack frame analysis in a real implementation
            return 0;
        }
    }
}