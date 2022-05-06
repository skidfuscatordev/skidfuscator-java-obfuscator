package dev.skidfuscator.obfuscator.skidasm.cfg;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.DefaultBlockFactory;
import org.mapleir.ir.cfg.builder.ssa.BlockBuilder;

public class SkidBlockFactory extends DefaultBlockFactory {
    public static final SkidBlockFactory INSTANCE = new SkidBlockFactory();

    @Override
    public BlockBuilder block() {
        return new BlockBuilder() {
            private ControlFlowGraph cfg;

            public BlockBuilder cfg(ControlFlowGraph cfg) {
                this.cfg = cfg;
                return this;
            }

            @Override
            public BasicBlock build() {
                assert cfg != null : "ControlFlowGraph cannot be null!";

                return new SkidBlock(cfg);
            }
        };
    }
}
