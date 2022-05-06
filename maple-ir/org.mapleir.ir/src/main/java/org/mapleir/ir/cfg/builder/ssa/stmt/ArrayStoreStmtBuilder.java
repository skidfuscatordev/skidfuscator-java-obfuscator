package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ArrayStoreStmt;

public interface ArrayStoreStmtBuilder extends Builder<ArrayStoreStmt> {
    ArrayStoreStmtBuilder array(final Expr expr);

    ArrayStoreStmtBuilder index(final Expr expr);

    ArrayStoreStmtBuilder value(final Expr expr);

    ArrayStoreStmtBuilder type(final TypeUtils.ArrayType type);
}
