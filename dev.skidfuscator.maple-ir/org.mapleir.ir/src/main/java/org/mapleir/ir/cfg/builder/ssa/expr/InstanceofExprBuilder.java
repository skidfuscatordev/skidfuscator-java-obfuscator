package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.CastExpr;
import org.mapleir.ir.code.expr.InstanceofExpr;
import org.objectweb.asm.Type;

public interface InstanceofExprBuilder extends Builder<InstanceofExpr> {
    InstanceofExprBuilder expr(final Expr expr);

    InstanceofExprBuilder type(final Type expr);
}
