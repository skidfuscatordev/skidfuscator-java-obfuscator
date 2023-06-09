package dev.skidfuscator.obfuscator.predicate.renderer.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeUnconditionalJumpStmt;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ConditionalJumpRenderer extends AbstractInstructionRenderer<ConditionalJumpStmt> {
    @Override
    public void transform(
            final Skidfuscator base,
            final ControlFlowGraph cfg,
            final ConditionalJumpStmt stmt) {
        final SkidBlock block = (SkidBlock) stmt.getBlock();
        final SkidMethodNode methodNode = (SkidMethodNode) cfg.getMethodNode();

        // Add jump and seed
        final SkidBlock seededBlock = (SkidBlock) block;
        final BasicBlock target = stmt.getTrueSuccessor();
        final SkidBlock targetSeeded = (SkidBlock) target;

        // Add jump and seed
        final SkidBlock basicBlock = new SkidBlock(block.cfg);
        methodNode.getFlowPredicate().set(basicBlock, targetSeeded.getSeed());
        basicBlock.setFlag(SkidBlock.FLAG_PROXY, true);

        methodNode.getFlowPredicate()
                .set(basicBlock, methodNode.getBlockPredicate(targetSeeded));

        final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(basicBlock, target);
        final UnconditionalJumpStmt proxy = new FakeUnconditionalJumpStmt(target, edge);
        proxy.setFlag(SkidBlock.FLAG_PROXY, true);

        basicBlock.add(proxy);

        // Add edge
        basicBlock.cfg.addVertex(basicBlock);
        basicBlock.cfg.addEdge(edge);

        // Replace successor
        stmt.setTrueSuccessor(basicBlock);
        basicBlock.cfg.addEdge(new ConditionalJumpEdge<>(block, basicBlock, Opcodes.IF_ICMPEQ));

        // Add seed loader
        this.addSeedLoader(
                methodNode,
                basicBlock,
                targetSeeded,
                0,
                methodNode.getFlowPredicate(),
                methodNode.getBlockPredicate(seededBlock),
                "Conditional"
        );

        if (IntegerBlockPredicateRenderer.DEBUG) {
            final Local local1 = basicBlock.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
            basicBlock.add(
                    1,
                    new SkidCopyVarStmt(
                            new VarExpr(local1, Type.getType(String.class)),
                            new ConstantExpr(
                                    block.getDisplayName()
                                            + " -> " + targetSeeded.getDisplayName()
                                            + " : c-loc - cond : "
                                            + methodNode.getBlockPredicate(targetSeeded)
                            )
                    )
            );
        }
    }
}
