package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.IRCache;

public class SkidCache extends IRCache {
    public SkidCache(final Skidfuscator skidfuscator) {
        super(e -> SkidFlowGraphBuilder.build(skidfuscator, e));
    }

    @Override
    public SkidControlFlowGraph getFor(MethodNode m) {
        return (SkidControlFlowGraph) super.getFor(m);
    }
}
