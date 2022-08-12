package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;

import org.objectweb.asm.Type;

public interface VarExprBuilder extends Builder<VarExpr> {
    VarExprBuilder local(final Local local);

    VarExprBuilder type(final Type expr);

    VarExprBuilder lifted(boolean expr);
}
