package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.FinalMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.PreMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.ImmediateEdge;
import org.mapleir.flowgraph.edges.SwitchEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.*;

public class FlatteningFlowTransformer extends AbstractTransformer {
    public FlatteningFlowTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "Control Flow Flattening");
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        final ControlFlowGraph cfg = methodNode.getCfg();
        for (BasicBlock block : new ArrayList<>(cfg.vertices())) {
            final BasicBlock immediate = cfg.getImmediate(block);

            if (immediate == null)
                continue;

            block.add(new UnconditionalJumpStmt(immediate));
        }
    }

    @Listen
    void handle(final FinalMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        final Skidfuscator skidfuscator = event.getSkidfuscator();

        final ControlFlowGraph cfg = methodNode.getCfg();

        final LinkedHashMap<Integer, BasicBlock> destinations = new LinkedHashMap<>();
        final SkidBlock skidBlock = new SkidBlock(cfg);

        final Set<BasicBlock> exempt = new HashSet<>();
        for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
            exempt.addAll(range.getNodes());
            exempt.add(range.getHandler());
        }

        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            if (exempt.contains(block))
                continue;
            final int seed = methodNode.getBlockPredicate((SkidBlock) block);
            destinations.put(seed, block);
            cfg.addEdge(new SwitchEdge<>(skidBlock, block, seed));
        }

        final SkidBlock entry = (SkidBlock) cfg.getEntries().iterator().next();
        final int startPredicate = methodNode.getBlockPredicate(entry);
        cfg.addEdge(new SwitchEdge<>(skidBlock, entry, startPredicate));
        cfg.addVertex(skidBlock);

        for (BasicBlock block : new ArrayList<>(cfg.vertices())) {
            if (exempt.contains(block))
                continue;

            final BasicBlock immediate = cfg.getImmediate(block);
            for (FlowEdge<BasicBlock> edge : new HashSet<>(cfg.getEdges(block))) {
                final boolean remove = edge instanceof UnconditionalJumpEdge
                        || edge instanceof ImmediateEdge;
                if (!remove)
                    continue;

                cfg.removeEdge(edge);
            }

            if (immediate != null)
                cfg.addEdge(new UnconditionalJumpEdge<>(block, immediate));
            cfg.addEdge(new UnconditionalJumpEdge<>(block, skidBlock));

            for (Stmt stmt : new ArrayList<>(block)) {
                if (stmt instanceof UnconditionalJumpStmt) {
                    final UnconditionalJumpStmt jump = (UnconditionalJumpStmt) stmt;
                    jump.setTarget(skidBlock);
                }
            }
        }

        skidBlock.add(new SwitchStmt(
                methodNode.getFlowPredicate().getGetter().get(cfg),
                destinations,
                skidBlock
        ));
    }
}
