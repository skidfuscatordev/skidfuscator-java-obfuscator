package dev.skidfuscator.obfuscator.transform.impl.vm.stmt;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.CaughtExceptionExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class VmCopyVarExceptionStmt extends CopyVarStmt {
    public VmCopyVarExceptionStmt(VarExpr variable, Expr expression) {
        super(variable, expression);
    }

    public VmCopyVarExceptionStmt(VarExpr variable, Expr expression, boolean synthetic) {
        super(variable, expression, synthetic);
    }

    @Override
    public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
        if(expression instanceof VarExpr) {
            if(((VarExpr) expression).getLocal() == variable.getLocal()) {
                return;
            }
        }

        variable.getLocal().setTempLocal(false);

        expression.toCode(visitor, assembler);
        Type type = variable.getType();
        if (TypeUtils.isPrimitive(type)) {
            int[] cast = TypeUtils.getPrimitiveCastOpcodes(expression.getType(), type);
            for (int i = 0; i < cast.length; i++)
                visitor.visitInsn(cast[i]);
        }

        Local local = variable.getLocal();
        if(local.isStack()) {
            if (expression instanceof CaughtExceptionExpr) {
                visitor.visitInsn(Opcodes.DUP_X2);
            }
            visitor.visitVarInsn(TypeUtils.getVariableStoreOpcode(getType()), variable.getLocal().getCodeIndex());
            if (expression instanceof CaughtExceptionExpr) {
                visitor.visitInsn(Opcodes.POP);
            }
            variable.getLocal().setTempLocal(true);
        } else {
            visitor.visitVarInsn(TypeUtils.getVariableStoreOpcode(getType()), variable.getLocal().getCodeIndex());
        }
    }
}
