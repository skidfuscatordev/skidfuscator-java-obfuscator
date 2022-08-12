package org.mapleir.ir.cfg.builder.ssa.stmt;

import org.mapleir.app.factory.Builder;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.ArrayStoreStmt;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;

public interface ConditionalJumpStmtBuilder extends Builder<ConditionalJumpStmt> {
    ConditionalJumpStmtBuilder left(final Expr expr);

    ConditionalJumpStmtBuilder right(final Expr expr);

    ConditionalJumpStmtBuilder target(final BasicBlock block);

    ConditionalJumpStmtBuilder type(final ConditionalJumpStmt.ComparisonType type);

    ConditionalJumpStmtBuilder edge(final ConditionalJumpEdge<BasicBlock> edge);
}
