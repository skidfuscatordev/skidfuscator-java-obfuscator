package dev.skidfuscator.obf.transform_legacy.number;

import org.mapleir.ir.code.Expr;

public interface NumberTransformer {
    Expr getNumber(final Number outcome, final Number starting, final Expr startingExpr);
}
