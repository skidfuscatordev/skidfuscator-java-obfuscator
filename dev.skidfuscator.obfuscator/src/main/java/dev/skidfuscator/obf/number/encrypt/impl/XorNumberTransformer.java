package dev.skidfuscator.obf.number.encrypt.impl;

import dev.skidfuscator.obf.maple.FakeArithmeticExpr;
import dev.skidfuscator.obf.number.encrypt.NumberTransformer;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class XorNumberTransformer implements NumberTransformer {
    @Override
    public Expr getNumber(final Number outcome, final Number starting, final Local startingExpr) {
        final int xored = outcome.intValue() ^ starting.intValue();
        final Expr allocExpr = new ConstantExpr(xored);
        return new FakeArithmeticExpr(allocExpr, new VarExpr(startingExpr, Type.INT_TYPE), ArithmeticExpr.Operator.XOR);
    }
}
