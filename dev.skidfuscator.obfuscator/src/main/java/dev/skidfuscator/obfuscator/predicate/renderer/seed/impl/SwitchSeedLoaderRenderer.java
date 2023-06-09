
package dev.skidfuscator.obfuscator.predicate.renderer.seed.impl;

import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlockFactory;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpStmt;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.DefaultSwitchEdge;
import org.mapleir.flowgraph.edges.SwitchEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.utils.CFGUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.LinkedHashMap;

public class SwitchSeedLoaderRenderer extends AbstractSeedLoaderRenderer {
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
        final boolean exceptionRange = type.contains("Exception Range");
        final SkidControlFlowGraph cfg = methodNode.getCfg();

        if (exceptionRange) {
            new InternalSeedLoaderRenderer()
                    .addSeedLoader(
                            methodNode,
                            block,
                            targetBlock,
                            index,
                            predicate,
                            value,
                            type
                    );
            return;
        }

        final int target = predicate.get(targetBlock);
        final PredicateFlowGetter getter = predicate.getGetter();
        final PredicateFlowSetter setter = predicate.getSetter();

        final HashTransformer hashTransformer = methodNode.getSkidfuscator().getLegacyHasher();
        final SkiddedHash hash = hashTransformer.hash(value, block, getter);

        final LinkedHashMap<Integer, BasicBlock> switcher = new LinkedHashMap<>();

        assert block.size() > 0 : "Failed???? " + type;

        final SkidBlock proxy = (SkidBlock) CFGUtils.splitBlockReverseFactory(
                SkidBlockFactory.v(methodNode.getSkidfuscator()),
                cfg,
                block,
                exceptionRange ? 1 : 0
        );
        proxy.setFlag(SkidBlock.FLAG_PROXY, true);
        final SkidBlock fuckup = cfg.getFuckup();

        switcher.put(hash.getHash(), proxy);
        cfg.addEdge(new SwitchEdge<>(block, proxy, hash.getHash()));

        for (BasicBlock crazy : new BasicBlock[] {
                block,
                targetBlock,
                fuckup
        }) {
            final int random = RandomUtil.nextInt();
            cfg.addEdge(new SwitchEdge<>(block, crazy, random));
            switcher.put(random, crazy);
        }

        cfg.addEdge(new DefaultSwitchEdge<>(block, fuckup));

        block.add(new SwitchStmt(
                hash.getExpr(),
                switcher,
                fuckup
        ));

        final Expr load = NumberManager.encrypt(
                target,
                value,
                block,
                getter
        );
        final Stmt set = setter.apply(load);
        proxy.add(
                0,
                set
        );

        cfg.recomputeEdges();
        //System.out.println(cfg.toString());
        //System.out.println(CFGUtils.printBlocks(Arrays.asList(block, proxy)));

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
