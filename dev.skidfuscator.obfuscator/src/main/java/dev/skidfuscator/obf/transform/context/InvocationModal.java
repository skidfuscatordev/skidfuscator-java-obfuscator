package dev.skidfuscator.obf.transform.context;

import dev.skidfuscator.obf.transform.caller.CallerType;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.Set;

@Data
public class InvocationModal {
    private final CallerType type;
    private final InvocationExpr expr;
    private final Set<MethodNode> methodNodes;

    public boolean isModifiable() {
        return type == CallerType.APPLICATION;
    }
}
