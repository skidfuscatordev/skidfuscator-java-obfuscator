package dev.skidfuscator.obfuscator.number.hash.impl;

import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

public class LegacyHashTransformer implements HashTransformer {
    @Override
    public SkiddedHash hash(int starting, final ControlFlowGraph cfg, PredicateFlowGetter caller) {
        final int hashed = hash(starting);
        final Expr hash = hash(cfg, caller);
        return new SkiddedHash(hash, hashed);
    }

    @Override
    public int hash(int starting) {
        return (((starting * 31) >>> 4) % starting) ^ (starting >>> 16);
    }

    @Override
    public Expr hash(final ControlFlowGraph cfg, PredicateFlowGetter caller) {
        // (starting * 31)
        final Expr var_load_a = caller.get(cfg);
        final Expr const_31 = new ConstantExpr(31, Type.INT_TYPE);
        final Expr arith_a = new FakeArithmeticExpr(var_load_a, const_31, ArithmeticExpr.Operator.MUL);

        // ((starting * 31) >>> 4)
        final Expr const_4 = new ConstantExpr(4, Type.INT_TYPE);
        final Expr arith_b = new FakeArithmeticExpr(arith_a, const_4, ArithmeticExpr.Operator.USHR);

        // ((starting * 31) >>> 4) % starting)
        final Expr var_load_b = caller.get(cfg);
        final Expr arith_c = new FakeArithmeticExpr(arith_b, var_load_b, ArithmeticExpr.Operator.REM);

        // (starting >>> 16)
        final Expr var_load_c = caller.get(cfg);
        final Expr const_16 = new ConstantExpr(16, Type.INT_TYPE);
        final Expr arith_d = new FakeArithmeticExpr(var_load_c, const_16, ArithmeticExpr.Operator.USHR);

        // (((starting * 31) >>> 4) % starting) ^ (starting >>> 16)
        final Expr hash = new FakeArithmeticExpr(arith_c, arith_d, ArithmeticExpr.Operator.XOR);

        return hash;
    }
}
