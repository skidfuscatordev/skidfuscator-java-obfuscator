package dev.skidfuscator.obfuscator.number.hash.impl;

import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

public class IntelliJHashTransformer implements HashTransformer {
    @Override
    public SkiddedHash hash(int starting, final BasicBlock vertex, PredicateFlowGetter caller) {
        final int hashed = hash(starting);
        final Expr hash = hash(vertex, caller);
        return new SkiddedHash(hash, hashed);
    }

    @Override
    public int hash(int starting) {
        return (starting ^ (starting >>> 16));
    }

    @Override
    public Expr hash(final BasicBlock vertex, PredicateFlowGetter caller) {
        // (starting >>> 16)
        final Expr var_calla = caller.get(vertex);
        final Expr const_29 = new ConstantExpr(16, Type.INT_TYPE);
        final Expr shifted7to29 = new FakeArithmeticExpr(var_calla, const_29, ArithmeticExpr.Operator.USHR);

        // (starting ^ (starting >>> 16))
        final Expr shiftedvartoshift = new FakeArithmeticExpr(
                caller.get(vertex),
                shifted7to29,
                ArithmeticExpr.Operator.XOR
        );
        return shiftedvartoshift;
    }
}
