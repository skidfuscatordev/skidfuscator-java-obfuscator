package dev.skidfuscator.obfuscator.skidasm;

import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.Invokable;

@Data
public class SkidInvocation {
    private final MethodNode owner;
    private Invokable expr;
    private boolean tainted;

    public SkidInvocation(MethodNode owner, Invokable expr) {
        this.owner = owner;
        this.expr = expr;
    }

    public Invokable getExpr() {
        return expr;
    }

    public void setExpr(Invokable expr) {
        this.expr = expr;
    }

    public Invocation asExpr() {
        return (Invocation) expr;
    }

    public boolean isDynamic() {
        return expr instanceof DynamicInvocationExpr;
    }

    public boolean isTainted() {
        return tainted;
    }

    public void setTainted(boolean tainted) {
        this.tainted = tainted;
    }
}
