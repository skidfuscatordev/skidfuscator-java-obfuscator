package dev.skidfuscator.obfuscator.transform.impl.vm;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.CaughtExceptionExpr;
import org.mapleir.ir.code.stmt.ArrayStoreStmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class VmArrayStoreStmt extends ArrayStoreStmt {
    public VmArrayStoreStmt(Expr arrayExpression, Expr indexExpression, Expr valueExpression, TypeUtils.ArrayType type) {
        super(arrayExpression, indexExpression, valueExpression, type);
    }

    @Override
    public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
        arrayExpression.toCode(visitor, assembler);
        indexExpression.toCode(visitor, assembler);
        int[] iCast = TypeUtils.getPrimitiveCastOpcodes(indexExpression.getType(), Type.INT_TYPE); // widen
        for (int i = 0; i < iCast.length; i++)
            visitor.visitInsn(iCast[i]);
        valueExpression.toCode(visitor, assembler);
        if (TypeUtils.isPrimitive(arrayType.getType())) {
//			System.out.println(this);
//			System.out.println(valueExpression.getType() + " -> " + type.getType());
            int[] vCast = TypeUtils.getPrimitiveCastOpcodes(valueExpression.getType(), arrayType.getType());
//			System.out.println("vcast: " + Arrays.toString(vCast));
            for (int i = 0; i < vCast.length; i++)
                visitor.visitInsn(vCast[i]);
        }
        if (valueExpression instanceof CaughtExceptionExpr) {
            visitor.visitInsn(Opcodes.DUP_X2);
            visitor.visitInsn(Opcodes.POP);
            visitor.visitInsn(Opcodes.DUP_X2);
            visitor.visitInsn(Opcodes.POP);
        }
        visitor.visitInsn(arrayType.getStoreOpcode());
    }
}
