package dev.skidfuscator.obfuscator.skidasm.cfg;

import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.exempt.BlockExempt;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;

public class SkidBlock extends BasicBlock {
    public static final int FLAG_NO_OPAQUE = 0x2;
    public static final int FLAG_PROXY = 0x4;
    public static final int FLAG_NO_EXCEPTION = 0x8;

    public SkidBlock(ControlFlowGraph cfg) {
        super(cfg);
    }

    public boolean isExempt(final BlockExempt... exemptions) {
        return BlockExempt.isExempt(this, exemptions);
    }

    public int getSeed() {
        return ((SkidMethodNode) cfg.getMethodNode()).getBlockPredicate(this);
    }
}
