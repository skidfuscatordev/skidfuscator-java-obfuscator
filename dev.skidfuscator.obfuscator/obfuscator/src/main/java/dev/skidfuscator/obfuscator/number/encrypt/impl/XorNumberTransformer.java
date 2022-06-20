package dev.skidfuscator.obfuscator.number.encrypt.impl;

import dev.skidfuscator.obfuscator.number.encrypt.NumberTransformer;
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

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class XorNumberTransformer implements NumberTransformer {
    @Override
    public Expr getNumber(
            final int outcome,
            final int starting,
            final BasicBlock vertex,
            final PredicateFlowGetter startingExpr) {
        final int xored = outcome ^ starting;
        final Expr allocExpr = new ConstantExpr(xored);
        return new FakeArithmeticExpr(
                allocExpr,
                startingExpr.get(vertex),
                ArithmeticExpr.Operator.XOR
        );
    }
}
