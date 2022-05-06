package org.mapleir.deob.passes;

import java.util.Map.Entry;

import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.stdlib.collections.graph.GraphUtils;
import org.mapleir.asm.MethodNode;

public class DetectIrreducibleFlowPass implements IPass {

	@Override
	public String getId() {
		return "Detect-Irreducible-Flow";
	}
	
	@Override
	public PassResult accept(PassContext pcxt) {
		AnalysisContext cxt = pcxt.getAnalysis();
		for(Entry<MethodNode, ControlFlowGraph> e : cxt.getIRCache().entrySet()) {
			MethodNode mn = e.getKey();
			ControlFlowGraph cfg = e.getValue();
			
			if(!GraphUtils.isReducibleGraph(cfg, cfg.getEntries().iterator().next())) {
				return PassResult.with(pcxt, this).fatal(new IllegalStateException(String.format("%s contains irreducible loop", mn))).make();
			}
		}
		return PassResult.with(pcxt, this).finished().make();
	}
}
