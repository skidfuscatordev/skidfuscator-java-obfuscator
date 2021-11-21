package dev.skidfuscator.obf.maple;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;

public class FakeBlock extends BasicBlock {
    public FakeBlock(ControlFlowGraph cfg) {
        super(cfg);
    }
}
