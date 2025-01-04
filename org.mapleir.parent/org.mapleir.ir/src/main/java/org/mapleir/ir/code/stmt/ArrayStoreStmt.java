package org.mapleir.ir.code.stmt;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.TypeUtils.ArrayType;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Expr.Precedence;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class ArrayStoreStmt extends Stmt {
	// TODO: Add validation
	protected Expr arrayExpression;
	protected Expr indexExpression;
	protected Expr valueExpression;
	protected ArrayType arrayType;

	public ArrayStoreStmt(Expr arrayExpression, Expr indexExpression, Expr valueExpression, ArrayType type) {
		super(ARRAY_STORE);
		this.setArrayType(type);
		this.setArrayExpression(arrayExpression);
		this.setIndexExpression(indexExpression);
		this.setValueExpression(valueExpression);
	}

	public void setArrayExpression(Expr arrayExpression) {
		if (this.arrayExpression != null) {
			this.arrayExpression.unlink();
		}

		this.arrayExpression = arrayExpression;
		this.arrayExpression.setParent(this);
	}

	public void setIndexExpression(Expr indexExpression) {
		if (this.indexExpression != null) {
			this.indexExpression.unlink();
		}

		this.indexExpression = indexExpression;
		this.indexExpression.setParent(this);
	}

	public void setValueExpression(Expr valueExpression) {
		if (this.valueExpression != null) {
			this.valueExpression.unlink();
		}

		this.valueExpression = valueExpression;
		this.valueExpression.setParent(this);
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		int accessPriority = Precedence.ARRAY_ACCESS.ordinal();
		int basePriority = arrayExpression.getPrecedence();
		if (basePriority > accessPriority)
			printer.print('(');
		arrayExpression.toString(printer);
		if (basePriority > accessPriority)
			printer.print(')');
		printer.print('[');
		indexExpression.toString(printer);
		printer.print(']');
		printer.print(" = ");
		valueExpression.toString(printer);
		printer.print(';');
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
		visitor.visitInsn(arrayType.getStoreOpcode());
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (valueExpression == previous) {
			this.setValueExpression(newest);
			return;
		} else if (indexExpression == previous) {
			this.setIndexExpression(newest);
			return;
		} else if (arrayExpression == previous) {
			this.setArrayExpression(newest);
			return;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public ArrayStoreStmt copy() {
		return new ArrayStoreStmt(arrayExpression.copy(), indexExpression.copy(), valueExpression.copy(), arrayType);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if (s instanceof ArrayStoreStmt) {
			ArrayStoreStmt store = (ArrayStoreStmt) s;
			return arrayExpression.equivalent(store.arrayExpression)
					&& indexExpression.equivalent(store.indexExpression)
					&& valueExpression.equivalent(store.valueExpression)
					&& arrayType.equals(store.arrayType);
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(valueExpression, arrayExpression, indexExpression);
	}
}