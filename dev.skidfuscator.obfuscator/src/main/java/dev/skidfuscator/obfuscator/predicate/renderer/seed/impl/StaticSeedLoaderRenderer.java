package dev.skidfuscator.obfuscator.predicate.renderer.seed.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.attribute.AttributeKey;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidInvocation;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpStmt;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class StaticSeedLoaderRenderer extends AbstractSeedLoaderRenderer {
    public static final AttributeKey LOADER = new AttributeKey("loader");

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

        final SkidClassNode parent = methodNode.getParent();

        if (!parent.hasAttribute(LOADER)) {
            final SkidMethodNode loader = parent
                    .createMethod()
                    .access(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)
                    .name(RandomUtil.randomAlphabeticalString(16))
                    .desc("(II)I")
                    .exceptions(new String[0])
                    .phantom(false)
                    .build();

            final LocalsPool pool = loader.getCfg().getLocals();
            final boolean swapped = RandomUtil.nextBoolean();

            final Local left = pool.get(0);
            final Local right = pool.get(1);

            loader.getCfg()
                    .getEntry()
                    .add(new ReturnStmt(
                            Type.INT_TYPE,
                            new ArithmeticExpr(
                                    new VarExpr(
                                            swapped ? left : right,
                                            Type.INT_TYPE
                                    ),
                                    new VarExpr(
                                            swapped ? right : left,
                                            Type.INT_TYPE
                                    ),
                                    ArithmeticExpr.Operator.XOR
                            )
                    ));

            loader.getGroup().addAttribute(LOADER, true);
            parent.addAttribute(LOADER, loader);
        }

        final SkidMethodNode node = parent.getAttribute(LOADER);

        final int target = predicate.get(targetBlock);
        final PredicateFlowGetter getter = predicate.getGetter();
        final PredicateFlowSetter setter = predicate.getSetter();

        final StaticInvocationExpr load = new StaticInvocationExpr(
                new Expr[]{
                        getter.get(block),
                            new ConstantExpr(value ^ target)
                },
                node.owner.node.name,
                node.node.name,
                node.node.desc
        );
        node.getGroup().getInvokers().add(
                new SkidInvocation(methodNode, load)
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
