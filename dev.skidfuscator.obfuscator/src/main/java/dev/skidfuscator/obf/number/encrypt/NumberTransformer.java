package dev.skidfuscator.obf.number.encrypt;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.locals.Local;

public interface NumberTransformer {
    Expr getNumber(final Number outcome, final Number starting, final Local startingExpr);
}
