package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.FieldStoreStmt;

public interface FieldStoreStmtBuilder extends Builder<FieldStoreStmt> {
    FieldStoreStmtBuilder instance(final Expr expr);

    FieldStoreStmtBuilder value(final Expr expr);

    FieldStoreStmtBuilder owner(final String owner);

    FieldStoreStmtBuilder name(final String name);

    FieldStoreStmtBuilder desc(final String desc);

    FieldStoreStmtBuilder statiz(final boolean statiz);
}
