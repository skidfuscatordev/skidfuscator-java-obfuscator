package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.PopStmt;

public interface PopStmtBuilder extends Builder<PopStmt> {
    PopStmtBuilder expr(final Expr expr);
}
