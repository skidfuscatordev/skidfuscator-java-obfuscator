package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.NegationExpr;

public interface NegationExprBuilder extends Builder<NegationExpr> {
    NegationExprBuilder expr(final Expr expr);
}
