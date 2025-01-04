package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.TypeUtils.ArrayType;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;

// TODO: Add validation
@Getter @Setter
public class ArrayLoadExpr extends Expr {
	
	private Expr arrayExpression;
	private Expr indexExpression;
	private ArrayType type;

	public ArrayLoadExpr(Expr array, Expr index, ArrayType type) {
		super(ARRAY_LOAD);
		this.type = type;
		this.setArrayExpression(array);
		this.setIndexExpression(index);
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

	@Override
	public Expr copy() {
		return new ArrayLoadExpr(arrayExpression.copy(), indexExpression.copy(), type);
	}

	@Override
	public Type getType() {
		return arrayExpression.getType().getSort() == Type.ARRAY
				? arrayExpression.getType().getElementType()
				: type.getType();
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
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
			this.setArrayExpression(newest);
		} else if (indexExpression == previous) {
			this.setIndexExpression(newest);
		} else {
			throw new IllegalArgumentException(String.format(
					"Cannot overwrite %s with %s in %s",
					previous, newest, this
			));
		}
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ArrayLoadExpr) {
			ArrayLoadExpr load = (ArrayLoadExpr) s;
			return arrayExpression.equals(load.arrayExpression) && indexExpression.equals(load.indexExpression);
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(arrayExpression, indexExpression);
	}
}