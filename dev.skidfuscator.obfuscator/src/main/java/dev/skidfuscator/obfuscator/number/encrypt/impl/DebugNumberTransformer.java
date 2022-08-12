package dev.skidfuscator.obfuscator.number.encrypt.impl;

import dev.skidfuscator.obfuscator.number.encrypt.NumberTransformer;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.locals.Local;

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class DebugNumberTransformer implements NumberTransformer {
    @Override
    public Expr getNumber(final int outcome, final int starting, final BasicBlock vertex, final PredicateFlowGetter startingExpr) {
        return new ConstantExpr(outcome);
    }
}
