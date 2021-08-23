package dev.skidfuscator.obf.transform_legacy.parameter.impl;

import dev.skidfuscator.obf.asm.MethodInvocation;
import dev.skidfuscator.obf.asm.MethodWrapper;
import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

@Getter
@Setter
public class ZelixInvocation extends MethodInvocation {
    public ZelixInvocation(MethodWrapper parent, MethodWrapper called, InvocationExpr expr) {
        super(parent, called, expr);
    }

    @Override
    public ZelixMethodWrapper getParent() {
        return (ZelixMethodWrapper) super.getParent();
    }

    @Override
    public ZelixMethodWrapper getCalled() {
        return (ZelixMethodWrapper) super.getCalled();
    }
}
