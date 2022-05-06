package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.SwitchStmt;

import java.util.LinkedHashMap;

public interface SwitchStmtBuilder extends Builder<SwitchStmt> {
    SwitchStmtBuilder expr(final Expr expr);

    SwitchStmtBuilder targets(final LinkedHashMap<Integer, BasicBlock> targets);

    SwitchStmtBuilder defaultTarget(final BasicBlock defaultBlock);
}
