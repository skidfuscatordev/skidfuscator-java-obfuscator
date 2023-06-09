package dev.skidfuscator.obfuscator.transform.impl.flow.exception;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.config.DefaultTransformerConfig;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidConstantExpr;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.transform.exempt.BlockExempt;
import dev.skidfuscator.obfuscator.transform.exempt.MethodExempt;
import dev.skidfuscator.obfuscator.transform.strategy.StrengthStrategy;
import dev.skidfuscator.obfuscator.transform.strategy.impl.AggressiveStrategy;
import dev.skidfuscator.obfuscator.transform.strategy.impl.NormalStrategy;
import dev.skidfuscator.obfuscator.transform.strategy.impl.WeakRandomSkipStrategy;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import dev.skidfuscator.obfuscator.verifier.alertable.AlertableConstantExpr;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicExceptionTransformer extends AbstractTransformer {
    public BasicExceptionTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public BasicExceptionTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"Flow Exception", children);
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        /*
         * Abstract methods should be skipped for this for obvious reasons.
         * Init because it causes issues
         * Null CFG because it causes issues
         */
        if (methodNode.isExempt(
                MethodExempt.ABSTRACT,
                MethodExempt.INIT,
                MethodExempt.NULLCFG
        ))
            return;

        final SkidControlFlowGraph cfg = methodNode.getCfg();
        final StrengthStrategy strategy;
        /*
         * Set up a strategy for obfuscation for exceptions. The following work as follows:
         * - Weak:          If was previously executed, run a random boolean, if last
         *                  wasn't, run
         *                  (75% average insertion rate)
         * - Good:          Always run once
         *                  (100% average insertion rate)
         * - Aggressive:    Always run
         *                  (200% average insertion rate)
         */
        switch (this.getConfig().getStrength()) {
            case WEAK: strategy = new WeakRandomSkipStrategy(); break;
            case GOOD: strategy = new NormalStrategy(); break;
            case AGGRESSIVE: strategy = new AggressiveStrategy(); break;
            default: throw new IllegalStateException(
                    String.format("Unknown strategy %s", this.getConfig().getStrength())
            );
        }

        final AtomicInteger changed = new AtomicInteger();
        for (SkidBlock entry : new HashSet<>(cfg.blocks())) {
            /*
             * Skip if we have empty blocks, proxy blocks, or exception
             * sensitive blocks (eg: polymorphic string encryption blocks)
             */
            if (entry.isExempt(
                    BlockExempt.EMPTY,
                    BlockExempt.NO_EXCEPT,
                    BlockExempt.NO_OPAQUE))
                continue;

            /*
             * I guess I owe some explanation to this.
             */
            if (!strategy.execute()) {
                strategy.reset();
                continue;
            }

            BasicBlock fuckup = cfg.getFuckup();

            // Todo make this a better system
            final int seed = methodNode.getBlockPredicate(entry);
            final SkiddedHash hash = NumberManager
                    .randomHasher(skidfuscator)
                    .hash(seed, entry, methodNode.getFlowPredicate().getGetter());

            // Todo add more boilerplates + add exception rotation

            final boolean doubleHit = strategy.execute();

            if (doubleHit && RandomUtil.nextBoolean() && changed.get() > 1) {
                fuckup = cfg.addFuckup("double");
            }

            /*
             * The following is to dissuade reverse engineers using call-graphs to
             * determine if a value is constantly static, so fuck u, im adding bogus
             * calls bitc
             */
            final Expr fuckupExpr = NumberManager.encrypt(
                    RandomUtil.nextInt(),
                    entry.getSeed(),
                    fuckup,
                    methodNode.getFlowPredicate().getGetter()
            );
            fuckupExpr.setBlock(fuckup);
            if (methodNode.isStatic() || RandomUtil.nextBoolean()) {
                // Fuckup the static value
                fuckup.add(0, methodNode.getParent().getStaticPredicate().getSetter().apply(
                        fuckupExpr
                ));
            } else {
                fuckup.add(0, methodNode.getParent().getClassPredicate().getSetter().apply(
                        fuckupExpr
                ));
            }

            // Todo change blocks to be skiddedblocks to add method to directly add these
            final ConstantExpr var_const = new AlertableConstantExpr(hash.getHash(), Type.INT_TYPE);
            final ConditionalJumpStmt jump_stmt = new FakeConditionalJumpStmt(hash.getExpr(), var_const, fuckup, ConditionalJumpStmt.ComparisonType.NE);

            if (entry.get(entry.size() - 1) instanceof UnconditionalJumpStmt)
                entry.add(entry.size() - 1, jump_stmt);
            else
                entry.add(jump_stmt);

            fuckup.add(
                    0,
                    new FakeConditionalJumpStmt(
                            hash.getExpr().copy(),
                            new SkidConstantExpr(RandomUtil.nextInt()),
                            fuckup,
                            ConditionalJumpStmt.ComparisonType.NE
                    )
            );

            if (doubleHit) {
                final ConstantExpr var_const_2 = new AlertableConstantExpr(
                        hash.getHash(),
                        Type.INT_TYPE
                );
                final ConditionalJumpStmt jump_stmt_2 = new FakeConditionalJumpStmt(
                        hash.getExpr().copy(),
                        var_const_2,
                        fuckup,
                        ConditionalJumpStmt.ComparisonType.NE
                );

                fuckup.add(0, jump_stmt_2);
            }

            cfg.recomputeEdges();
            event.tick();

            // Regular debug
            if (IntegerBlockPredicateRenderer.DEBUG) {
                final Local local1 = entry.cfg.getLocals().get(entry.cfg.getLocals().getMaxLocals() + 2);
                entry.add(entry.indexOf(jump_stmt), new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                        new ConstantExpr(entry.getDisplayName() + " : var expect: " + var_const.getConstant())));
            }

            changed.getAndIncrement();
            strategy.reset();
        }
    }

    @Override
    protected <T extends DefaultTransformerConfig> T createConfig() {
        return (T) new BasicExceptionConfig(
                skidfuscator.getTsConfig(),
                MiscUtil.toCamelCase(name)
        );
    }

    @Override
    public BasicExceptionConfig getConfig() {
        return (BasicExceptionConfig) super.getConfig();
    }
}
