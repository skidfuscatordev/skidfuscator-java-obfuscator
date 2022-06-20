package org.mapleir.ir.codegen;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Label;

public interface BytecodeFrontend {
	Label getLabel(BasicBlock b);

	ControlFlowGraph getGraph();
}
