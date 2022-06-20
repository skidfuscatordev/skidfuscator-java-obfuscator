package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.expr.CastExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.objectweb.asm.Type;

public interface ConstantExprBuilder extends Builder<ConstantExpr> {
    ConstantExprBuilder cst(final Object cst);

    ConstantExprBuilder type(final Type expr);

    ConstantExprBuilder check(final boolean check);
}
