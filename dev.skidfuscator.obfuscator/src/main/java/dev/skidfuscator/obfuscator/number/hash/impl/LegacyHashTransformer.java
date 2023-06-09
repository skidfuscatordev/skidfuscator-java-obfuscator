package dev.skidfuscator.obfuscator.number.hash.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.builder.SkidMethodNodeBuilder;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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

        // (starting * 31)
        final Expr var_load_a = new VarExpr(cfg.getLocals().get(0), Type.INT_TYPE);
        final Expr const_31 = new ConstantExpr(31, Type.INT_TYPE);
        final Expr arith_a = new FakeArithmeticExpr(var_load_a, const_31, ArithmeticExpr.Operator.MUL);

        // ((starting * 31) >>> 4)
        final Expr const_4 = new ConstantExpr(4, Type.INT_TYPE);
        final Expr arith_b = new FakeArithmeticExpr(arith_a, const_4, ArithmeticExpr.Operator.USHR);

        // ((starting * 31) >>> 4) % starting)
        final Expr var_load_b = new VarExpr(cfg.getLocals().get(0), Type.INT_TYPE);
        final Expr arith_c = new FakeArithmeticExpr(arith_b, var_load_b, ArithmeticExpr.Operator.REM);

        // (starting >>> 16)
        final Expr var_load_c = new VarExpr(cfg.getLocals().get(0), Type.INT_TYPE);
        final Expr const_16 = new ConstantExpr(16, Type.INT_TYPE);
        final Expr arith_d = new FakeArithmeticExpr(var_load_c, const_16, ArithmeticExpr.Operator.USHR);

        // (((starting * 31) >>> 4) % starting) ^ (starting >>> 16)
        final Expr hash = new FakeArithmeticExpr(arith_c, arith_d, ArithmeticExpr.Operator.XOR);

        final BasicBlock cond = new SkidBlock(cfg);
        cfg.addVertex(cond);
        methodNode.getCfg().getEntry().add(new ConditionalJumpStmt(
                new VarExpr(cfg.getLocals().get(0), Type.INT_TYPE),
                new ConstantExpr(0),
                cond,
                ConditionalJumpStmt.ComparisonType.EQ
        ));
        cond.add(new ReturnStmt(Type.INT_TYPE, new ConstantExpr(0)));
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
        return (((starting * 31) >>> 4) % starting) ^ (starting >>> 16);
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
