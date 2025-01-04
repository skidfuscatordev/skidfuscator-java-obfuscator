package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class ArrayLengthExpr extends Expr {

	// TODO: Add validation
	private Expr expression;

	public ArrayLengthExpr(Expr expression) {
		super(ARRAY_LEN);
		this.setExpression(expression);
	}

	public void setExpression(Expr expression) {
		if (this.expression != null) {
			this.expression.unlink();
		}

		this.expression = expression;
		this.expression.setParent(this);
	}

	@Override
	public Expr copy() {
		return new ArrayLengthExpr(expression.copy());
	}

	@Override
	public Type getType() {
		return Type.INT_TYPE;
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public Precedence getPrecedence0() {
		return Precedence.MEMBER_ACCESS;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		int selfPriority = getPrecedence();
		int expressionPriority = expression.getPrecedence();
		if (expressionPriority > selfPriority)
			printer.print('(');
		expression.toString(printer);
		if (expressionPriority > selfPriority)
			printer.print(')');
		printer.print(".length");
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		expression.toCode(visitor, assembler);
		visitor.visitInsn(Opcodes.ARRAYLENGTH);
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (expression == previous) {
			this.setExpression(newest);
		} else {
			throw new IllegalArgumentException(String.format(
					"Cannot overwrite %s with %s in %s",
					previous, newest, this
			));
		}
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		return (s instanceof ArrayLengthExpr) && expression.equivalent(((ArrayLengthExpr)s).expression);
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(expression);
	}
}