package org.mapleir.ir.cfg.builder;

import org.mapleir.ir.utils.CFGUtils;

public class DeadBlocksPass extends ControlFlowGraphBuilder.BuilderPass {

	public DeadBlocksPass(ControlFlowGraphBuilder builder) {
		super(builder);
	}

	@Override
	public void run() {
		assert(builder.graph.getEntries().size() == 1);
		builder.head = CFGUtils.deleteUnreachableBlocks(builder.graph);
	}
}
