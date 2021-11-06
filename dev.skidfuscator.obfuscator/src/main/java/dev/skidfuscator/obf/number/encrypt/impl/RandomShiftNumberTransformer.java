package dev.skidfuscator.obf.number.encrypt.impl;

import dev.skidfuscator.obf.maple.FakeArithmeticExpr;
import dev.skidfuscator.obf.number.encrypt.NumberTransformer;
import dev.skidfuscator.obf.utils.RandomUtil;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

public class RandomShiftNumberTransformer implements NumberTransformer {
    @Override
    public Expr getNumber(Number outcome, Number starting, Local startingExpr) {
        Expr expr = new VarExpr(startingExpr, Type.INT_TYPE);
        int firstSeed = starting.intValue();

        boolean switcher = false;

        for (int i = 0; i < RandomUtil.nextInt(4); i++) {
            switch (RandomUtil.nextInt(3)) {
                case 0: {
                    if (switcher) {
                        firstSeed = firstSeed >> starting.intValue();
                        expr = new FakeArithmeticExpr(expr, new VarExpr(startingExpr, Type.INT_TYPE), ArithmeticExpr.Operator.SHR);
                    } else {
                        final byte seed = (byte) (RandomUtil.nextInt(127) + 1);
                        firstSeed = firstSeed >> seed;
                        expr = new FakeArithmeticExpr(expr, new ConstantExpr(seed, Type.INT_TYPE), ArithmeticExpr.Operator.SHR);
                    }

                    break;
                }
                case 1: {
                    if (switcher) {
                        firstSeed = firstSeed << starting.intValue();
                        expr = new FakeArithmeticExpr(expr, new VarExpr(startingExpr, Type.INT_TYPE), ArithmeticExpr.Operator.SHL);
                    } else {
                        final byte seed = (byte) (RandomUtil.nextInt(127) + 1);
                        firstSeed = firstSeed << seed;
                        expr = new FakeArithmeticExpr(expr, new ConstantExpr(seed, Type.INT_TYPE), ArithmeticExpr.Operator.SHL);
                    }
                    break;
                }
                case 2: {
                    if (switcher) {
                        firstSeed = firstSeed & starting.intValue();
                        expr = new FakeArithmeticExpr(expr, new VarExpr(startingExpr, Type.INT_TYPE), ArithmeticExpr.Operator.AND);
                    } else {
                        final byte seed = (byte) (RandomUtil.nextInt(254) + 1);
                        firstSeed = firstSeed & seed;
                        expr = new FakeArithmeticExpr(expr, new ConstantExpr(seed, Type.INT_TYPE), ArithmeticExpr.Operator.AND);
                    }
                    break;
                }
            }

            switcher = !switcher;
        }

        int xor = outcome.intValue() ^ firstSeed;
        expr = new FakeArithmeticExpr(new ConstantExpr(xor, Type.INT_TYPE), expr, ArithmeticExpr.Operator.XOR);

        return expr;
    }
}
