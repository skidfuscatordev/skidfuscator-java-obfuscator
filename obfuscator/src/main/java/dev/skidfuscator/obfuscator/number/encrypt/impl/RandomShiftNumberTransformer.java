package dev.skidfuscator.obfuscator.number.encrypt.impl;

import dev.skidfuscator.obfuscator.number.encrypt.NumberTransformer;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

public class RandomShiftNumberTransformer implements NumberTransformer {
    @Override
    public Expr getNumber(int outcome, int starting, final BasicBlock vertex, PredicateFlowGetter startingExpr) {
        Expr expr = startingExpr.get(vertex);
        int firstSeed = starting;

        boolean switcher = false;

        for (int i = 0; i < RandomUtil.nextInt(4); i++) {
            switch (RandomUtil.nextInt(3)) {
                case 0: {
                    if (switcher) {
                        firstSeed = firstSeed >> starting;
                        expr = new FakeArithmeticExpr(expr, startingExpr.get(vertex), ArithmeticExpr.Operator.SHR);
                    } else {
                        final byte seed = (byte) (RandomUtil.nextInt(127) + 1);
                        firstSeed = firstSeed >> seed;
                        expr = new FakeArithmeticExpr(expr, new ConstantExpr(seed, Type.INT_TYPE), ArithmeticExpr.Operator.SHR);
                    }

                    break;
                }
                case 1: {
                    if (switcher) {
                        firstSeed = firstSeed << starting;
                        expr = new FakeArithmeticExpr(expr, startingExpr.get(vertex), ArithmeticExpr.Operator.SHL);
                    } else {
                        final byte seed = (byte) (RandomUtil.nextInt(127) + 1);
                        firstSeed = firstSeed << seed;
                        expr = new FakeArithmeticExpr(expr, new ConstantExpr(seed, Type.INT_TYPE), ArithmeticExpr.Operator.SHL);
                    }
                    break;
                }
                case 2: {
                    if (switcher) {
                        firstSeed = firstSeed & starting;
                        expr = new FakeArithmeticExpr(expr, startingExpr.get(vertex), ArithmeticExpr.Operator.AND);
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

        int xor = outcome ^ firstSeed;
        expr = new FakeArithmeticExpr(new ConstantExpr(xor, Type.INT_TYPE), expr, ArithmeticExpr.Operator.XOR);

        return expr;
    }
}
