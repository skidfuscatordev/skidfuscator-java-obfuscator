package org.mapleir.ir.cfg.builder.ssa;

import org.mapleir.app.factory.Builder;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.locals.LocalsPool;

public interface CfgBuilder extends Builder<ControlFlowGraph> {
    CfgBuilder localsPool(final LocalsPool localsPool);

    CfgBuilder method(final MethodNode methodNode);
}
