package dev.skidfuscator.obfuscator.predicate.renderer.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeUnconditionalJumpStmt;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import org.mapleir.flowgraph.edges.SwitchEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SwitchJumpRenderer extends AbstractInstructionRenderer<SwitchStmt> {
    @Override
    public void transform(
            final Skidfuscator base,
            final ControlFlowGraph cfg,
            final SwitchStmt stmt) {
        final SkidBlock currentBlock = (SkidBlock) stmt.getBlock();
        final SkidMethodNode methodNode = (SkidMethodNode) cfg.getMethodNode();

        final SkidBlock seededBlock = (SkidBlock) currentBlock;

        final LinkedHashMap<Integer, BasicBlock> targets = new LinkedHashMap<>();
        final Set<Integer> completed = new HashSet<>();
        for (Map.Entry<Integer, BasicBlock> entry : stmt.getTargets().entrySet()) {
            final int seed = entry.getKey();
            final BasicBlock value = entry.getValue();
            if (value == stmt.getDefaultTarget())
                continue;

            if (completed.contains(seed))
                continue;

            completed.add(seed);

            final SkidBlock target = (SkidBlock) value;
            // Add jump and seed
            final SkidBlock basicBlock = new SkidBlock(value.cfg);
            basicBlock.setFlag(SkidBlock.FLAG_PROXY, true);
            methodNode.getFlowPredicate()
                    .set(basicBlock, methodNode.getBlockPredicate(target));

            final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(basicBlock, target);
            final UnconditionalJumpStmt proxy = new FakeUnconditionalJumpStmt(target, edge);
            proxy.setFlag(SkidBlock.FLAG_PROXY, true);

            basicBlock.add(proxy);

            // Add edge
            basicBlock.cfg.addVertex(basicBlock);
            basicBlock.cfg.addEdge(edge);

            // Replace successor
            targets.put(seed, basicBlock);
            basicBlock.cfg.addEdge(new SwitchEdge<>(seededBlock, basicBlock, stmt.getOpcode()));

            // Add seed loader
            this.addSeedLoader(
                    methodNode,
                    basicBlock,
                    target,
                    0,
                    methodNode.getFlowPredicate(),
                    methodNode.getBlockPredicate(seededBlock),
                    "Switch Entry [" + entry.getKey() + ", og:" + target.getDisplayName()
                            + ", redirected: " + basicBlock.getDisplayName() + ")"
            );

            if (IntegerBlockPredicateRenderer.DEBUG) {
                final Local local1 = basicBlock.cfg.getLocals().get(seededBlock.cfg.getLocals().getMaxLocals() + 2);
                basicBlock.add(
                        1,
                        new SkidCopyVarStmt(
                                new VarExpr(local1, Type.getType(String.class)),
                                new ConstantExpr(
                                        seededBlock.getDisplayName()
                                                + " -> " + target.getDisplayName()
                                                + " : c-loc - switch : "
                                                + methodNode.getBlockPredicate(target)
                                )
                        )
                );
            }
        }

        stmt.setTargets(targets);

        if (stmt.getDefaultTarget() == null || stmt.getDefaultTarget() == currentBlock)
            return;

        final SkidBlock target = (SkidBlock) stmt.getDefaultTarget();
        // Add jump and seed
        final SkidBlock basicBlock = new SkidBlock(target.cfg);
        basicBlock.setFlag(SkidBlock.FLAG_PROXY, true);
        methodNode.getFlowPredicate().set(basicBlock, methodNode.getBlockPredicate(target));

        final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(basicBlock, target);
        final UnconditionalJumpStmt proxy = new FakeUnconditionalJumpStmt(target, edge);
        proxy.setFlag(SkidBlock.FLAG_PROXY, true);

        basicBlock.add(proxy);

        // Add edge
        basicBlock.cfg.addVertex(basicBlock);
        basicBlock.cfg.addEdge(edge);

        // Replace successor
        stmt.setDefaultTarget(basicBlock);
        basicBlock.cfg.addEdge(new SwitchEdge<>(seededBlock, basicBlock, stmt.getOpcode()));

        // Add seed loader
        this.addSeedLoader(
                methodNode,
                basicBlock,
                target,
                0,
                methodNode.getFlowPredicate(),
                methodNode.getBlockPredicate(seededBlock),
                "Switch Default"
        );

        if (IntegerBlockPredicateRenderer.DEBUG) {
            final Local local1 = basicBlock.cfg.getLocals().get(seededBlock.cfg.getLocals().getMaxLocals() + 2);
            basicBlock.add(
                    1,
                    new SkidCopyVarStmt(
                            new VarExpr(local1, Type.getType(String.class)),
                            new ConstantExpr(
                                    seededBlock.getDisplayName()
                                            + " -> " + target.getDisplayName()
                                            + " : c-loc - switch : "
                                            + methodNode.getBlockPredicate(target)
                            )
                    )
            );
        }
    }
}
