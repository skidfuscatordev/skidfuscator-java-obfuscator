package dev.skidfuscator.obfuscator.number.hash.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.builder.SkidMethodNodeBuilder;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.function.Supplier;

public class LegacyHashTransformer implements HashTransformer {
    private final SkidMethodNode methodNode;

    public LegacyHashTransformer(Skidfuscator skidfuscator) {
        this.methodNode = new SkidMethodNodeBuilder(skidfuscator, skidfuscator.getFactoryNode())
                .desc("(I)I")
                .name(RandomUtil.randomAlphabeticalString(16))
                .access(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC)
                .phantom(true)
                .build();

        final SkidControlFlowGraph cfg = methodNode.getCfg();

        // Invertible 32-bit finalizer (MurmurHash3 fmix32).
        // Every step -- an xor-shift or a multiply by an odd constant -- is a bijection over the 32-bit integers,
        // so the whole function is a permutation of int.
        // That matters because SwitchTransformer stores the hashed case keys in a LinkedHashMap:
        // the previous `(((x*31)>>>4) % x) ^ (x>>>16)` was not injective (and divided by zero at x==0),
        // so two distinct case keys could hash to the same value, the later Map.put dropped the earlier case,
        // and an obfuscated switch silently mis-routed one branch to another.
        // A bijection can never collide two keys.
        // x ^= x >>> 16; x *= 0x85ebca6b; x ^= x >>> 13; x *= 0xc2b2ae35; x ^= x >>> 16;
        final Supplier<Expr> x =
                () -> new VarExpr(cfg.getLocals().get(0), Type.INT_TYPE);
        final Supplier<Expr> s1 = () -> new FakeArithmeticExpr(
                x.get(),
                new FakeArithmeticExpr(x.get(), new ConstantExpr(16, Type.INT_TYPE), ArithmeticExpr.Operator.USHR),
                ArithmeticExpr.Operator.XOR);
        final Supplier<Expr> s2 = () -> new FakeArithmeticExpr(
                s1.get(), new ConstantExpr(0x85ebca6b, Type.INT_TYPE), ArithmeticExpr.Operator.MUL);
        final Supplier<Expr> s3 = () -> new FakeArithmeticExpr(
                s2.get(),
                new FakeArithmeticExpr(s2.get(), new ConstantExpr(13, Type.INT_TYPE), ArithmeticExpr.Operator.USHR),
                ArithmeticExpr.Operator.XOR);
        final Supplier<Expr> s4 = () -> new FakeArithmeticExpr(
                s3.get(), new ConstantExpr(0xc2b2ae35, Type.INT_TYPE), ArithmeticExpr.Operator.MUL);
        final Expr hash = new FakeArithmeticExpr(
                s4.get(),
                new FakeArithmeticExpr(s4.get(), new ConstantExpr(16, Type.INT_TYPE), ArithmeticExpr.Operator.USHR),
                ArithmeticExpr.Operator.XOR);

        methodNode.getCfg().getEntry().add(new ReturnStmt(
                Type.INT_TYPE,
                hash
        ));
        methodNode.dump();
        methodNode.node.maxLocals = 1;
    }
    @Override
    public SkiddedHash hash(int starting, final BasicBlock vertex, PredicateFlowGetter caller) {
        final int hashed = hash(starting);
        final Expr hash = hash(vertex, caller);
        return new SkiddedHash(hash, hashed);
    }

    @Override
    public int hash(int starting) {
        // MurmurHash3 fmix32
        // must stay identical to the bytecode emitted in the constructor.
        starting ^= starting >>> 16;
        starting *= 0x85ebca6b;
        starting ^= starting >>> 13;
        starting *= 0xc2b2ae35;
        starting ^= starting >>> 16;
        return starting;
    }

    @Override
    public Expr hash(final BasicBlock vertex, PredicateFlowGetter caller) {
        return new StaticInvocationExpr(
                new Expr[] {caller.get(vertex)},
                methodNode.getOwner(),
                methodNode.getName(),
                methodNode.getDesc()
        );
    }
}
