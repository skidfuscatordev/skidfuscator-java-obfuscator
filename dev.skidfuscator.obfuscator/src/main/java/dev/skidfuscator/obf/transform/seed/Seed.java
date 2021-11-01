package dev.skidfuscator.obf.transform.seed;

import dev.skidfuscator.obf.transform.flow.gen3.SkidGraph;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;

import java.util.List;
import java.util.Set;

public interface Seed<T> {
    void renderPrivate(final MethodNode methodNode, final ControlFlowGraph cfg);
    void renderPublic(final List<SkidGraph> methodNodes);

    T getPublic();
    T getPrivate();
    Expr getPrivateLoader();
}
