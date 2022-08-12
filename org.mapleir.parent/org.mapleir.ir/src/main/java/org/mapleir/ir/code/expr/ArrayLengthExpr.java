package org.mapleir.ir.code.expr;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ArrayLengthExpr extends Expr {

	private Expr expression;

	public ArrayLengthExpr(Expr expression) {
		super(ARRAY_LEN);
		setExpression(expression);
	}

	public Expr getExpression() {
		return expression;
	}

	public void setExpression(Expr expression) {
		writeAt(expression, 0);
	}

	@Override
	public Expr copy() {
		return new ArrayLengthExpr(expression.copy());
	}

	@Override
	public Type getType() {
		return Type.INT_TYPE;
	}

	@Override
	public void onChildUpdated(int ptr) {
		if(ptr == 0) {
			expression = read(0);
		} else {
			raiseChildOutOfBounds(ptr);
		}
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
			expression = newest;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		return (s instanceof ArrayLengthExpr) && expression.equivalent(((ArrayLengthExpr)s).expression);
	}
}