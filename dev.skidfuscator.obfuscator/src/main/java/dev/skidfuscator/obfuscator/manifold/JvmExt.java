package dev.skidfuscator.obfuscator.manifold;

import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.objectweb.asm.Type;

public enum JvmExt {
    push,
    pop,
    invoke;



    public Expr prefixBind(Integer expr) {
        return _push(expr, Type.INT_TYPE);
    }

    public Expr _push(Object cst, Type type) {
        if (this != push) {
            throw new IllegalStateException("Cannot prefix bind a pop operation");
        }

        return new ConstantExpr(cst, type);
    }
}
