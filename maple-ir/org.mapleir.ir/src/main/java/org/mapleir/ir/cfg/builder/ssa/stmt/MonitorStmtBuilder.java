package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.MonitorStmt;

public interface MonitorStmtBuilder extends Builder<MonitorStmt> {
    MonitorStmtBuilder expr(final Expr expr);

    MonitorStmtBuilder mode(final MonitorStmt.MonitorMode expr);

}
