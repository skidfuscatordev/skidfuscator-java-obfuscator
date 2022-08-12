package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.FieldLoadExpr;

public interface FieldLoadExprBuilder extends Builder<FieldLoadExpr> {
    FieldLoadExprBuilder instance(final Expr expr);

    FieldLoadExprBuilder owner(final String owner);

    FieldLoadExprBuilder name(final String name);

    FieldLoadExprBuilder desc(final String desc);

    FieldLoadExprBuilder statiz(final boolean statiz);

}
