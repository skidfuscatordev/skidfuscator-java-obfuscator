package dev.skidfuscator.obfuscator.transform.exempt;

import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import org.mapleir.asm.MethodNode;

import java.util.function.Function;

public enum MethodExempt {
    ABSTRACT(MethodNode::isAbstract),
    INTERFACE(m -> m.getParent().isInterface()),
    INIT(MethodNode::isInit),
    CLINIT(MethodNode::isClinit),
    NULLCFG(methodNode -> methodNode.getCfg() == null)
    ;

    private final Function<SkidMethodNode, Boolean> function;

    MethodExempt(Function<SkidMethodNode, Boolean> function) {
        this.function = function;
    }

    public boolean isExempt(final SkidMethodNode methodNode) {
        return function.apply(methodNode);
    }

    public static boolean isExempt(final SkidMethodNode methodNode, final MethodExempt... exemption) {
        for (MethodExempt methodExemption : exemption) {
            if (methodExemption.isExempt(methodNode)) {
                return true;
            }
        }

        return false;
    }
}
