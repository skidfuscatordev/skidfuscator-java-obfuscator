package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.expr.CaughtExceptionExpr;
import org.objectweb.asm.Type;

public interface CaughtExceptionExprBuilder extends Builder<CaughtExceptionExpr> {
    CaughtExceptionExprBuilder type(final String type);
}
