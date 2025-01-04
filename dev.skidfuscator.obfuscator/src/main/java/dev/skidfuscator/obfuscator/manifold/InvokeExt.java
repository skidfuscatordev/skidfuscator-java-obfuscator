package dev.skidfuscator.obfuscator.manifold;

import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;

public enum InvokeExt {
    invoke;

    public Expr prefixBind(SkidMethodNode parent, Expr... args) {
        if (this != invoke) {
            throw new IllegalStateException("Cannot prefix bind a pop or push operation");
        }

        if (parent.isStatic()) {
            return new StaticInvocationExpr(parent.getParent().isInterface()
                    ? InvocationExpr.CallType.INTERFACE : InvocationExpr.CallType.STATIC,
                    args,
                    parent.getOwner(),
                    parent.getName(),
                    parent.getDesc()
            );
        } else {
            InvocationExpr.CallType callType;

            if (parent.getParent().isInterface()) {
                callType = InvocationExpr.CallType.INTERFACE;
            } else if (parent.isInit()) {
                callType = InvocationExpr.CallType.SPECIAL;
            } else {
                callType = InvocationExpr.CallType.VIRTUAL;
            }

            return new VirtualInvocationExpr(
                    callType,
                    args,
                    parent.getOwner(),
                    parent.getName(),
                    parent.getDesc()
            );
        }
    }
}
