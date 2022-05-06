package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.CastExpr;
import org.mapleir.ir.code.expr.ComparisonExpr;

public interface ComparisonExprBuilder extends Builder<ComparisonExpr> {
    ComparisonExprBuilder left(final Expr left);

    ComparisonExprBuilder right(final Expr right);

    ComparisonExprBuilder type(final ComparisonExpr.ValueComparisonType expr);
}
