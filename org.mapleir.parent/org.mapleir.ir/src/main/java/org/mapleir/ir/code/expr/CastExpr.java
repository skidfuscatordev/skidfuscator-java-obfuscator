package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

@Getter @Setter
public class CastExpr extends Expr {

	// TODO: Add validation
	private Expr expression;
	private Type type;

	public CastExpr(Expr expression, Type type) {
		super(CAST);
		this.setType(type);
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
		return new CastExpr(expression.copy(), type);
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public Precedence getPrecedence0() {
		return Precedence.CAST;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		int selfPriority = getPrecedence();
		int exprPriority = expression.getPrecedence();
		printer.print('(');
		printer.print(type.getClassName());
		printer.print(')');
		if (exprPriority > selfPriority) {
			printer.print('(');
		}
		expression.toString(printer);
		if (exprPriority > selfPriority) {
			printer.print(')');
		}
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		expression.toCode(visitor, assembler);
		if (TypeUtils.isObjectRef(getType())) {
			visitor.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
		} else {
			int[] instructions = TypeUtils.getPrimitiveCastOpcodes(expression.getType(), type);
			for (int i = 0; i < instructions.length; i++) {
				visitor.visitInsn(instructions[i]);
			}
		}
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
		if(s instanceof CastExpr) {
			CastExpr cast = (CastExpr) s;
			return expression.equivalent(cast) && type.equals(cast.type);
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(expression);
	}
}