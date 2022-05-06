package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.CastExpr;
import org.objectweb.asm.Type;

public interface CastExprBuilder extends Builder<CastExpr> {
    CastExprBuilder expr(final Expr expr);

    CastExprBuilder type(final Type type);
}
