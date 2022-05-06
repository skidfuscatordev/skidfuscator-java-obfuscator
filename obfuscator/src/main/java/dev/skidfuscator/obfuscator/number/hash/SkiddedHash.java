package dev.skidfuscator.obfuscator.number.hash;

import lombok.Data;
import org.mapleir.ir.code.Expr;

@Data
public class SkiddedHash {
    private final Expr expr;
    private final int hash;

    public SkiddedHash(Expr expr, int hash) {
        this.expr = expr;
        this.hash = hash;
    }
}
