package org.mapleir.ir.cfg.builder.ssa.stmt.copy;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyPhiStmt;

public interface CopyPhiStmtBuilder extends Builder<CopyPhiStmt> {
    CopyPhiStmtBuilder var(final VarExpr varExpr);

    CopyPhiStmtBuilder phi(final PhiExpr phiExpr);
}
