package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.PhiExceptionExpr;

import java.util.Map;

public interface PhiExceptionExprBuilder extends Builder<PhiExceptionExpr> {
    PhiExceptionExprBuilder args(final Map<BasicBlock, Expr> arguments);
}
