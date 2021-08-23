package dev.skidfuscator.obf.transform.yggdrasil;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

@Data
@AllArgsConstructor
public class SkidInvocation {
    private final SkidMethod methodNode;
    private final InvocationExpr invocationExpr;
}
