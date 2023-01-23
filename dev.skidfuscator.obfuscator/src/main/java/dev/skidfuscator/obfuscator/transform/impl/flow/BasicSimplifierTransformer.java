package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventPriority;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.*;

public class BasicSimplifierTransformer extends AbstractTransformer {
    public BasicSimplifierTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "Block Simplifier");
    }

    @Listen(EventPriority.MONITOR)
    void handle(final InitMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        methodNode.getEntryBlock();

        final ControlFlowGraph cfg = methodNode.getCfg();
        for (BasicBlock block : new ArrayList<>(cfg.vertices())) {
            final BasicBlock immediate = cfg.getImmediate(block);

            /*if (block.getPool() != null) {
                final Local local1 = cfg.getLocals().get(cfg.getLocals().getMaxLocals() + 2);
                block.add(
                        0,
                        new SkidCopyVarStmt(
                                new VarExpr(local1, Type.getType(String.class)),
                                new ConstantExpr(
                                        "[Frame] " + Arrays.toString(block.getPool().getRenderedTypes())
                                )
                        )
                );
            }*/

            if (immediate == null)
                continue;

            cfg.removeEdge(cfg.getImmediateEdge(block));

            final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(block, immediate);
            cfg.addEdge(edge);
            block.add(new UnconditionalJumpStmt(immediate, edge));
        }

        cfg.recomputeEdges();

        for (BasicBlock vertex : cfg.vertices()) {
            assert cfg.getImmediateEdge(vertex) == null : "Block has an immediate edge after removal?";
        }
    }

}
