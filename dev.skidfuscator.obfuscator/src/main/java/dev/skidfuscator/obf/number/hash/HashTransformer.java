package dev.skidfuscator.obf.number.hash;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.locals.Local;

public interface HashTransformer {
    SkiddedHash hash(final int starting, final Local caller);

    int hash (final int starting);

    Expr hash(final Local expr);
}
