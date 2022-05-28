package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;

import java.util.*;

public class BasicSimplifierTransformer extends AbstractTransformer {
    public BasicSimplifierTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "Block Simplifier");
    }

    @Listen
    void handle(final InitMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        final ControlFlowGraph cfg = methodNode.getCfg();
        for (BasicBlock block : new ArrayList<>(cfg.vertices())) {
            final BasicBlock immediate = cfg.getImmediate(block);

            if (immediate == null)
                continue;

            cfg.removeEdge(cfg.getImmediateEdge(block));

            final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(block, immediate);
            cfg.addEdge(edge);
            block.add(new UnconditionalJumpStmt(immediate, edge));
        }
    }

}
