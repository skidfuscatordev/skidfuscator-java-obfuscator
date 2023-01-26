package dev.skidfuscator.obfuscator.skidasm.fake;

import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;

public class FakeConditionalJumpStmt extends ConditionalJumpStmt {
    public FakeConditionalJumpStmt(Expr left, Expr right, BasicBlock trueSuccessor, ComparisonType type) {
        super(left, right, trueSuccessor, type);
    }

    public FakeConditionalJumpStmt(Expr left, Expr right, BasicBlock trueSuccessor, ComparisonType type, ConditionalJumpEdge<BasicBlock> edge) {
        super(left, right, trueSuccessor, type, edge);
    }
}
