package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.CastExpr;
import org.mapleir.ir.code.expr.NewArrayExpr;
import org.objectweb.asm.Type;

public interface NewArrayExprBuilder extends Builder<NewArrayExpr> {
    NewArrayExprBuilder bounds(final Expr[] expr);

    NewArrayExprBuilder type(final Type expr);
}
