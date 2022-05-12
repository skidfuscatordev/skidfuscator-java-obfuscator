package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.FinalMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.PreMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
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
import java.util.stream.Collectors;

public class FlatteningFlowTransformer extends AbstractTransformer {
    public FlatteningFlowTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "Control Flow Flattening");
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

    @Listen
    void handle(final FinalMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        final Skidfuscator skidfuscator = event.getSkidfuscator();

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        if (cfg.size() <= 3) {
            return;
        }

        if (methodNode.isAbstract() || methodNode.isNative())
            return;

        final BlockOpaquePredicate opaquePredicate = methodNode.getFlowPredicate();

        assert opaquePredicate != null : "Flow Predicate is null?";
        final PredicateFlowGetter getter = opaquePredicate.getGetter();

        // TODO: Figure why this is happening
        if (getter == null)
            return;

        final Set<BasicBlock> exempt = new HashSet<>();
        for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
            exempt.addAll(range.getNodes());
            exempt.add(range.getHandler());
        }

        for (BasicBlock vertex : cfg.vertices()) {
            if (vertex.getStack() == null)
                exempt.add(vertex);
        }

        if (cfg.vertices()
                .stream()
                .filter(exempt::contains)
                .flatMap(Collection::stream)
                .noneMatch(e -> e instanceof UnconditionalJumpStmt))
            return;

        final LinkedHashMap<Integer, BasicBlock> destinations = new LinkedHashMap<>();
        final SkidBlock dispatcherBlock = new SkidBlock(cfg);
        cfg.addVertex(dispatcherBlock);

        new HashSet<>(cfg.vertices())
                .stream()
                .filter(exempt::contains)
                .flatMap(Collection::stream)
                .filter(e -> e instanceof UnconditionalJumpStmt)
                .map(e -> (UnconditionalJumpStmt) e)
                .forEach(e -> {
                    final SkidBlock currentBlock = (SkidBlock) e.getBlock();
                    final SkidBlock oldTarget = (SkidBlock) e.getTarget();

                    /* Replace target block */
                    e.setTarget(dispatcherBlock);

                    /* Replace edge */
                    final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(currentBlock, dispatcherBlock);
                    cfg.removeEdge(e.getEdge());
                    cfg.addEdge(edge);
                    e.setEdge(edge);

                    final int seed = methodNode.getBlockPredicate(oldTarget);
                    destinations.put(seed, oldTarget);
                    cfg.addEdge(new SwitchEdge<>(dispatcherBlock, oldTarget, seed));
                });

        dispatcherBlock.add(new SwitchStmt(
                getter.get(cfg),
                destinations,
                dispatcherBlock
        ));
    }
}
