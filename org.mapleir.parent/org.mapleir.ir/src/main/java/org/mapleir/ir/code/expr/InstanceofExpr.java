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

import java.util.List;

@Getter @Setter
public class InstanceofExpr extends Expr {

	// TODO: Add validation
	private Expr expression;
	private Type checkType;

	public InstanceofExpr(Expr expression, Type type) {
		super(INSTANCEOF);
		this.checkType = type;
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
		return new InstanceofExpr(expression.copy(), checkType);
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public Type getType() {
		return Type.BOOLEAN_TYPE;
	}

	@Override
	public Precedence getPrecedence0() {
		return Precedence.LE_LT_GE_GT_INSTANCEOF;
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
		printer.print(" instanceof ");
		printer.print(checkType.getClassName());
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		expression.toCode(visitor, assembler);
		visitor.visitTypeInsn(Opcodes.INSTANCEOF, checkType.getInternalName());
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (expression == previous) {
			this.setExpression(newest);
		}

		super.overwrite(previous, newest);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof InstanceofExpr) {
			InstanceofExpr e = (InstanceofExpr) s;
			return expression.equivalent(e.expression) && checkType.equals(e.checkType);
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(expression);
	}
}