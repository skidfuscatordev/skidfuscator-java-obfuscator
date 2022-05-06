package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.ThrowStmt;

public interface ThrowStmtBuilder extends Builder<ThrowStmt> {
    ThrowStmtBuilder expr(final Expr expr);

}
