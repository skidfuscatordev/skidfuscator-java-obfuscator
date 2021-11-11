package dev.skidfuscator.obf.skidasm.v2;

import lombok.Data;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

@Data
public class SInvocation {
    private final InvocationExpr expr;

    public SInvocation(InvocationExpr expr) {
        this.expr = expr;
    }
}
