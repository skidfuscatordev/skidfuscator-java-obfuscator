package dev.skidfuscator.obfuscator.skidasm.fake;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;

public class FakeBlock extends BasicBlock {
    public FakeBlock(ControlFlowGraph cfg) {
        super(cfg);
    }
}
