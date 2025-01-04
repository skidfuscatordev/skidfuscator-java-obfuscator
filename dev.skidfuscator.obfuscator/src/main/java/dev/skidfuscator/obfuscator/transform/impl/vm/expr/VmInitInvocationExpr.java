package dev.skidfuscator.obfuscator.transform.impl.vm.expr;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.objectweb.asm.MethodVisitor;

public class VmInitInvocationExpr extends VirtualInvocationExpr {
    public VmInitInvocationExpr(CallType callType, Expr[] args, String owner, String name, String desc) {
        super(callType, args, owner, name, desc);
    }

    @Override
    public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
        super.toCode(visitor, assembler);

    }
}
