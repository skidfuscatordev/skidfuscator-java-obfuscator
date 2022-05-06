package org.mapleir.deob.passes;

import org.mapleir.asm.MethodNode;
import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;

import java.util.HashSet;
import java.util.Map;

public class DeadBlockRemoverPass implements IPass {
    @Override
    public PassResult accept(PassContext pcxt) {
        AnalysisContext cxt = pcxt.getAnalysis();

        int removed = 0;

        for(ControlFlowGraph cfg : cxt.getIRCache().values()) {
            for (BasicBlock b : new HashSet<>(cfg.vertices())) {
                if (cfg.getReverseEdges(b).size() == 0 && !cfg.getEntries().contains(b)) {
                    cfg.removeVertex(b);
                    removed++;
                }
            }
        }

        System.out.printf("  removed %d dead blocks.%n", removed);
        return PassResult.with(pcxt, this).finished(removed).make();
    }
}
