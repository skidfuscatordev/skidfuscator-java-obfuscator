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

public class IntelliJHashTransformer implements HashTransformer {
    @Override
    public SkiddedHash hash(int starting, Local caller) {
        final int hashed = hash(starting);
        final Expr hash = hash(caller);
        return new SkiddedHash(hash, hashed);
    }

    @Override
    public int hash(int starting) {
        return (starting ^ (starting >>> 16));
    }

    @Override
    public Expr hash(Local caller) {
        // (starting >>> 16)
        final Expr var_calla = new VarExpr(caller, Type.INT_TYPE);
        final Expr const_29 = new ConstantExpr(16, Type.INT_TYPE);
        final Expr shifted7to29 = new FakeArithmeticExpr(var_calla, const_29, ArithmeticExpr.Operator.USHR);

        // (starting ^ (starting >>> 16))
        final Expr shiftedvartoshift = new FakeArithmeticExpr(new VarExpr(caller, Type.INT_TYPE), shifted7to29, ArithmeticExpr.Operator.XOR);
        return shiftedvartoshift;
    }
}
