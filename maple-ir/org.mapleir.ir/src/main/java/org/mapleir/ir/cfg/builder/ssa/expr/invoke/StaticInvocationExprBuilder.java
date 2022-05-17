package org.mapleir.ir.cfg.builder.ssa.expr.invoke;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;

public interface StaticInvocationExprBuilder extends Builder<StaticInvocationExpr> {
    StaticInvocationExprBuilder callType(InvocationExpr.CallType callType);
    StaticInvocationExprBuilder args(Expr[] args);
    StaticInvocationExprBuilder owner(String owner);
    StaticInvocationExprBuilder name(String name);
    StaticInvocationExprBuilder desc(String desc);
}
