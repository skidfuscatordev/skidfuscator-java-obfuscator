package org.mapleir.ir.cfg.builder.ssa;

import org.mapleir.app.factory.Builder;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;

public interface BlockBuilder extends Builder<BasicBlock> {
    BlockBuilder cfg(final ControlFlowGraph cfg);
}
