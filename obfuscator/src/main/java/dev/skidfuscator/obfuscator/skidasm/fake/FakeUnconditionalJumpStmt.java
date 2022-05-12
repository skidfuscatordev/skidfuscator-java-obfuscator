package dev.skidfuscator.obfuscator.skidasm.fake;

import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;

public class FakeUnconditionalJumpStmt extends UnconditionalJumpStmt {
    public FakeUnconditionalJumpStmt(BasicBlock target, UnconditionalJumpEdge<BasicBlock> edge) {
        super(target, edge);
    }
}
