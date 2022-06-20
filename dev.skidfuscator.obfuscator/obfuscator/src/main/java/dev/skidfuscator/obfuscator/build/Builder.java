package dev.skidfuscator.obfuscator.build;

import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.SSAFactory;
import org.mapleir.ir.code.Expr;

import java.util.Stack;

public class Builder {
    private final SSAFactory factory;
    private final ControlFlowGraph cfg;
    private final Stack<Expr> expr;

    public Builder(SSAFactory factory, ControlFlowGraph cfg) {
        this.factory = factory;
        this.cfg = cfg;
        this.expr = new Stack<>();
    }

    public BlockBuilder block() {
        return new BlockBuilder(factory, cfg, expr);
    }


}
