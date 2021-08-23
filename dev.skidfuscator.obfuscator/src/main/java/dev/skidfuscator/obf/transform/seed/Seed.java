package dev.skidfuscator.obf.transform.seed;

import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;

import java.util.List;
import java.util.Set;

public interface Seed<T> {
    void renderPrivate(final MethodNode methodNode, final ControlFlowGraph cfg);
    void renderPublic(final List<MethodNode> methodNodes);

    T getPublic();
    Expr getPrivate();
}
