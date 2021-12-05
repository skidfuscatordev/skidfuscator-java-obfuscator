package dev.skidfuscator.obf.creator;

import org.mapleir.asm.MethodNode;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;

public class SkidFlowGraphBuilder extends ControlFlowGraphBuilder {
    public SkidFlowGraphBuilder(MethodNode method) {
        super(method);
    }

    public SkidFlowGraphBuilder(MethodNode method, boolean optimise) {
        super(method, optimise);
    }

    public static ControlFlowGraph build(MethodNode method) {
        ControlFlowGraphBuilder builder = new ControlFlowGraphBuilder(method);
        final ControlFlowGraph cfg = builder.buildImpl();
        BoissinotDestructor.leaveSSA(cfg);
        LocalsReallocator.realloc(cfg);

        return cfg;
    }
}
