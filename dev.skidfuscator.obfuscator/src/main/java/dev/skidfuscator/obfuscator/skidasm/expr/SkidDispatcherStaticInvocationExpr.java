package dev.skidfuscator.obfuscator.skidasm.expr;

import dev.skidfuscator.obfuscator.transform.impl.j2c.NativeTransformer;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.objectweb.asm.MethodVisitor;

public class SkidDispatcherStaticInvocationExpr extends StaticInvocationExpr {
    public SkidDispatcherStaticInvocationExpr(Expr[] args, String owner, String name, String desc) {
        super(args, owner, name, desc);
    }

    public SkidDispatcherStaticInvocationExpr(CallType callType, Expr[] args, String owner, String name, String desc) {
        super(callType, args, owner, name, desc);
    }

    @Override
    public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
        new PopStmt(new StaticInvocationExpr(
                new Expr[0],
                "skid/Dispatcher",
                "load",
                "()V"
        )).toCode(visitor, assembler);

        super.toCode(visitor, assembler);
    }
}
