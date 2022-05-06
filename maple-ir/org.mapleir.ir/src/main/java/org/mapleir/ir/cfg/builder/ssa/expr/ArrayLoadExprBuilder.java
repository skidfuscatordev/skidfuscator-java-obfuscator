package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArrayLoadExpr;

public interface ArrayLoadExprBuilder extends Builder<ArrayLoadExpr> {
    ArrayLoadExprBuilder array(final Expr expr);

    ArrayLoadExprBuilder index(final Expr expr);

    ArrayLoadExprBuilder type(final TypeUtils.ArrayType type);
}
