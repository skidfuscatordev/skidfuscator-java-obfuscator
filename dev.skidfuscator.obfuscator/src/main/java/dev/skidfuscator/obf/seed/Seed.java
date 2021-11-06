package dev.skidfuscator.obf.seed;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.locals.Local;

import java.util.List;

public interface Seed<T> {
    void renderPrivate(final MethodNode methodNode, final ControlFlowGraph cfg);
    void renderPublic(final List<SkidGraph> methodNodes, final SkidSession session);

    T getPublic();
    T getPrivate();
    Expr getPrivateLoader();
    Local getLocal();
}
