package dev.skidfuscator.obfuscator.predicate.renderer.seed.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpStmt;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ExceptionSeedLoaderRenderer extends AbstractSeedLoaderRenderer {
    @Override
    public void addSeedLoader(
            final SkidMethodNode methodNode,
            final SkidBlock block,
            final SkidBlock targetBlock,
            final int index,
            final BlockOpaquePredicate predicate,
            final int value,
            final String type
    ) {
        final int target = predicate.get(targetBlock);
        final PredicateFlowGetter getter = predicate.getGetter();
        final PredicateFlowSetter setter = predicate.getSetter();

        final Expr load = NumberManager.encrypt(
                target,
                value,
                block,
                getter
        );
        final Stmt set = setter.apply(load);

        block.add(
                index < 0 ? block.size() : index,
                set
        );
        if (IntegerBlockPredicateRenderer.DEBUG) {
            final BasicBlock exception = Blocks.exception(
                    block.cfg,
                    block.getDisplayName() + " --> "
                            + targetBlock.getDisplayName()
                            + " # Failed to match seed of type "
                            + type
                            + " and value "
                            + target
            );

            final Stmt jumpStmt = new FakeConditionalJumpStmt(
                    getter.get(targetBlock),
                    new ConstantExpr(
                            target,
                            Type.INT_TYPE
                    ),
                    exception,
                    ConditionalJumpStmt.ComparisonType.NE
            );
            block.cfg.addEdge(
                    new ConditionalJumpEdge<>(
                            block,
                            exception,
                            Opcodes.GOTO
                    )
            );
            block.add(
                    index < 0 ? block.size() : index + 1,
                    jumpStmt
            );
        }
    }
}
