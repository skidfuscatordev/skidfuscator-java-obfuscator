package org.mapleir.ir.code.expr;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.TypeUtils.ArrayType;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ArrayLoadExpr extends Expr {
	
	private Expr arrayExpression;
	private Expr indexExpression;
	private ArrayType type;

	public ArrayLoadExpr(Expr array, Expr index, ArrayType type) {
		super(ARRAY_LOAD);
		this.type = type;
		setArrayExpression(array);
		setIndexExpression(index);
	}

	public Expr getArrayExpression() {
		return arrayExpression;
	}

	public void setArrayExpression(Expr arrayExpression) {
		writeAt(arrayExpression, 0);
	}

	public Expr getIndexExpression() {
		return indexExpression;
	}

	public void setIndexExpression(Expr indexExpression) {
		writeAt(indexExpression, 1);
	}

	public ArrayType getArrayType() {
		return type;
	}

	public void setArrayType(ArrayType type) {
		this.type = type;
	}

	@Override
	public Expr copy() {
		return new ArrayLoadExpr(arrayExpression.copy(), indexExpression.copy(), type);
	}

	@Override
	public Type getType() {
		return type.getType();
	}

	@Override
	public void onChildUpdated(int ptr) {
		if (ptr == 0) {
			arrayExpression = read(0);
		} else if (ptr == 1) {
			indexExpression = read(1);
		} else {
			raiseChildOutOfBounds(ptr);
		}
	}

	@Override
	public Precedence getPrecedence0() {
		return Precedence.ARRAY_ACCESS;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		int selfPriority = getPrecedence();
		int expressionPriority = arrayExpression.getPrecedence();
		if (expressionPriority > selfPriority) {
			printer.print('(');
		}
		arrayExpression.toString(printer);
		if (expressionPriority > selfPriority) {
			printer.print(')');
		}
		printer.print('[');
		indexExpression.toString(printer);
		printer.print(']');
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		arrayExpression.toCode(visitor, assembler);
		indexExpression.toCode(visitor, assembler);
//		System.out.println("the:  " + index.getId() + ". "+ index);
//		System.out.println("  par:   " + getRootParent().getId() + ". "+ getRootParent());
		int[] iCast = TypeUtils.getPrimitiveCastOpcodes(indexExpression.getType(), Type.INT_TYPE);
		for (int i = 0; i < iCast.length; i++) {
			visitor.visitInsn(iCast[i]);
		}
		visitor.visitInsn(type.getLoadOpcode());
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (arrayExpression == previous) {
			arrayExpression = newest;
		} else if (indexExpression == previous) {
			indexExpression = newest;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ArrayLoadExpr) {
			ArrayLoadExpr load = (ArrayLoadExpr) s;
			return arrayExpression.equals(load.arrayExpression) && indexExpression.equals(load.indexExpression);
		}
		return false;
	}
}