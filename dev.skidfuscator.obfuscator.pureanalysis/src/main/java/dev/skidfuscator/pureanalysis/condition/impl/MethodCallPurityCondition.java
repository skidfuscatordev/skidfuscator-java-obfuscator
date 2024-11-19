package dev.skidfuscator.pureanalysis.condition.impl;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.PurityCondition;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

public class MethodCallPurityCondition extends PurityCondition {
    private final Set<String> whitelist = new HashSet<>();
    private final boolean allowRecursion;
    
    public MethodCallPurityCondition(boolean allowRecursion) {
        super("Method call");
        this.allowRecursion = allowRecursion;
    }
    
    public void addWhitelistedMethod(String methodSignature) {
        whitelist.add(methodSignature);
    }
    
    @Override
    public boolean evaluate(MethodNode method, ClassNode classNode, PurityAnalyzer analyzer) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                String signature = methodInsn.owner + "." + methodInsn.name + methodInsn.desc;
                
                // Allow recursive calls if configured
                if (allowRecursion && 
                    methodInsn.owner.equals(classNode.name) && 
                    methodInsn.name.equals(method.name) && 
                    methodInsn.desc.equals(method.desc)) {
                    continue;
                }
                
                // Check whitelist and analyzer
                if (!whitelist.contains(signature) && 
                    !analyzer.isPureMethod(methodInsn.owner, methodInsn.name, methodInsn.desc)) {
                    return false;
                }
            }
        }
        return evaluateNested(method, classNode, analyzer);
    }
}