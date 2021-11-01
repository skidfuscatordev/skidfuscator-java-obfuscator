package dev.skidfuscator.obf.seed;

import dev.skidfuscator.obf.skidasm.SkidGraph;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;

import java.util.List;

public interface Seed<T> {
    void renderPrivate(final MethodNode methodNode, final ControlFlowGraph cfg);
    void renderPublic(final List<SkidGraph> methodNodes);

    T getPublic();
    T getPrivate();
    Expr getPrivateLoader();
}
