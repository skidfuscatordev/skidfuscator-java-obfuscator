package dev.skidfuscator.obfuscator.skidasm.cfg;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;

public class SkidBlock extends BasicBlock {
    public static final int FLAG_NO_OPAQUE = 0x2;

    public SkidBlock(ControlFlowGraph cfg) {
        super(cfg);
    }
}
