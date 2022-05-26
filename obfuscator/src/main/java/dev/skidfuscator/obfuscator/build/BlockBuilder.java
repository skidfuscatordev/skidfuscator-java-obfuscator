package dev.skidfuscator.obfuscator.build;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.SSAFactory;
import org.mapleir.ir.code.Expr;

import java.util.Stack;

public class BlockBuilder {
    private final SSAFactory factory;
    private final BasicBlock block;
    private final Stack<Expr> stack;

    public BlockBuilder(SSAFactory factory, ControlFlowGraph cfg, Stack<Expr> expr) {
        this.factory = factory;
        this.block = factory.block()
                .cfg(cfg)
                .build();
        this.stack = expr;
    }

    public ConditionBuilder condition() {
        return new ConditionBuilder(factory, block.cfg, block, stack);
    }
}
