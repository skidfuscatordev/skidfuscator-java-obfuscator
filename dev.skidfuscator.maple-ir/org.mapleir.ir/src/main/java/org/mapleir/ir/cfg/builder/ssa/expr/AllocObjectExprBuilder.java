package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.objectweb.asm.Type;

public interface AllocObjectExprBuilder extends Builder<AllocObjectExpr> {
    AllocObjectExprBuilder type(final Type type);
}
