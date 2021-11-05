package dev.skidfuscator.obf.number.hash.impl;

import dev.skidfuscator.obf.maple.FakeArithmeticExpr;
import dev.skidfuscator.obf.number.hash.HashTransformer;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

public class BitwiseHashTransformer implements HashTransformer {
    @Override
    public SkiddedHash hash(int starting, Local caller) {
        final int hashed = hash(starting);
        final Expr hash = hash(caller);
        return new SkiddedHash(hash, hashed);
    }

    @Override
    public int hash(int starting) {
        return ((starting & (7 << 29)) >> 29) | (starting << 3);
    }

    @Override
    public Expr hash(Local caller) {
        // (7 << 29)
        final Expr const7 = new ConstantExpr(7, Type.INT_TYPE);
        final Expr const29a = new ConstantExpr(29, Type.INT_TYPE);
        final Expr shifted7to29 = new FakeArithmeticExpr(const7, const29a, ArithmeticExpr.Operator.SHL);

        // (starting & (7 << 29))
        final Expr shiftedvartoshift = new FakeArithmeticExpr(new VarExpr(caller, Type.INT_TYPE), shifted7to29, ArithmeticExpr.Operator.AND);

        // (starting & (7 << 29)) >> 29)
        final Expr const29b = new ConstantExpr(29, Type.INT_TYPE);
        final Expr shiftedHashTto29 = new FakeArithmeticExpr(shiftedvartoshift, const29b, ArithmeticExpr.Operator.SHR);

        // (starting << 3)
        final Expr const3 = new ConstantExpr(3, Type.INT_TYPE);
        final Expr shiftedStartTo3 = new FakeArithmeticExpr(new VarExpr(caller, Type.INT_TYPE), const3, ArithmeticExpr.Operator.SHL);

        // ((starting & (7 << 29)) >> 29) | (starting << 3);
        final Expr hash = new FakeArithmeticExpr(shiftedHashTto29, shiftedStartTo3, ArithmeticExpr.Operator.OR);

        return hash;
    }
}
