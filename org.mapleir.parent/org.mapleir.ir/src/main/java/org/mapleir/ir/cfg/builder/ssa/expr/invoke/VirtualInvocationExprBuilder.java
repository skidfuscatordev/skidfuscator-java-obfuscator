package org.mapleir.ir.cfg.builder.ssa.expr.invoke;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;

public interface VirtualInvocationExprBuilder extends Builder<VirtualInvocationExpr> {
    VirtualInvocationExprBuilder callType(InvocationExpr.CallType callType);
    VirtualInvocationExprBuilder args(Expr[] args);
    VirtualInvocationExprBuilder owner(String owner);
    VirtualInvocationExprBuilder name(String name);
    VirtualInvocationExprBuilder desc(String desc);
}
