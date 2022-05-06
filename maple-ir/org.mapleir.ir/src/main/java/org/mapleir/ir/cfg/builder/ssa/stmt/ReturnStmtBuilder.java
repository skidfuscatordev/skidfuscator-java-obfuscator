package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.objectweb.asm.Type;

public interface ReturnStmtBuilder extends Builder<ReturnStmt> {
    ReturnStmtBuilder type(final Type expr);

    ReturnStmtBuilder expr(final Expr expr);
}
