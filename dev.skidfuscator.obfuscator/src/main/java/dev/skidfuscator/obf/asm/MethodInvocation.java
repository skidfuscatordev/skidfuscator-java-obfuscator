package dev.skidfuscator.obf.asm;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.Objects;

@Getter
@Setter
public class MethodInvocation {
    private MethodWrapper parent;
    private MethodWrapper called;
    private InvocationExpr expr;

    public MethodInvocation(MethodWrapper parent, MethodWrapper called, InvocationExpr expr) {
        this.parent = parent;
        this.called = called;
        this.expr = expr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInvocation that = (MethodInvocation) o;
        return Objects.equals(parent, that.parent) && Objects.equals(called, that.called) && Objects.equals(expr, that.expr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent.getHash(), called.getHash(), expr);
    }
}
