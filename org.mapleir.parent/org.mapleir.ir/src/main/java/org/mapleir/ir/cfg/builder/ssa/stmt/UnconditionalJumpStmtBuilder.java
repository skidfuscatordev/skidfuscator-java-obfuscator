package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;

public interface UnconditionalJumpStmtBuilder extends Builder<UnconditionalJumpStmt> {
    UnconditionalJumpStmtBuilder target(final BasicBlock target);

    UnconditionalJumpStmtBuilder edge(UnconditionalJumpEdge<BasicBlock> edge);
}
