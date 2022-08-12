package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;

public interface ArithmeticExprBuilder extends Builder<ArithmeticExpr> {
    ArithmeticExprBuilder left(final Expr left);

    ArithmeticExprBuilder right(final Expr right);

    ArithmeticExprBuilder operator(final ArithmeticExpr.Operator operator);
}
