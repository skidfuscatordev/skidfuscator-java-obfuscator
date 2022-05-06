package dev.skidfuscator.obfuscator.skidasm.cfg;

import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.impl.IntegerBlockOpaquePredicate;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;

public class SkidBlock extends BasicBlock {
    public SkidBlock(ControlFlowGraph cfg) {
        super(cfg);
    }
}
