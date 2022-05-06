package org.mapleir.ir.cfg.builder.ssa.expr;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.CastExpr;
import org.mapleir.ir.code.expr.PhiExpr;

import java.util.Map;

public interface PhiExprBuilder extends Builder<PhiExpr> {
    PhiExprBuilder args(final Map<BasicBlock, Expr> arguments);
}
