package dev.skidfuscator.obf.number.encrypt;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.locals.Local;

public interface NumberTransformer {
    Expr getNumber(final int outcome, final int starting, final Local startingExpr);
}
