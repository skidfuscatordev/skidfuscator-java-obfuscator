package dev.skidfuscator.obf.skidasm;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

@Data
@AllArgsConstructor
public class SkidInvocation {
    private final SkidMethod methodNode;
    private final InvocationExpr invocationExpr;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkidInvocation that = (SkidInvocation) o;

        if (methodNode != null ? !methodNode.equals(that.methodNode) : that.methodNode != null) return false;
        return invocationExpr != null ? invocationExpr.equals(that.invocationExpr) : that.invocationExpr == null;
    }

    @Override
    public int hashCode() {
        int result = methodNode != null ? methodNode.hashCode() : 0;
        result = 31 * result + (invocationExpr != null ? invocationExpr.hashCode() : 0);
        return result;
    }
}
