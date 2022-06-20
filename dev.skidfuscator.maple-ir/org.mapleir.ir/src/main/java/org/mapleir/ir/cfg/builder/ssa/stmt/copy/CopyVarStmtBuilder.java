package org.mapleir.ir.cfg.builder.ssa.stmt.copy;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;

public interface CopyVarStmtBuilder extends Builder<CopyVarStmt> {
    CopyVarStmtBuilder var(final VarExpr varExpr);

    CopyVarStmtBuilder expr(final Expr expr);

    CopyVarStmtBuilder synthetic(final boolean synthetic);
}
