package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArrayLengthExpr;

public interface ArrayLengthExprBuilder extends Builder<ArrayLengthExpr> {
    ArrayLengthExprBuilder expr(final Expr expr);
}
