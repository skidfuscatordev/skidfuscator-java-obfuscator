package org.mapleir.ir.code.stmt;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class ReturnStmt extends Stmt {
	// TODO: Add validation
	private Type type;
	private Expr expression;

	public ReturnStmt() {
		this(Type.VOID_TYPE, null);
	}

	public ReturnStmt(Type type, Expr expression) {
		super(RETURN);
		this.type = type;
		this.setExpression(expression);
	}

	public void setExpression(Expr expression) {
		if (this.expression != null) {
			this.expression.unlink();
		}

		this.expression = expression;
		if (expression != null)
			expression.setParent(this);
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		if (expression != null) {
			printer.print("return ");
			expression.toString(printer);
			printer.print(';');
		} else {
			printer.print("return;");
		}
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		if (type != Type.VOID_TYPE) {
			expression.toCode(visitor, assembler);
			if (TypeUtils.isPrimitive(type)) {
				int[] cast = TypeUtils.getPrimitiveCastOpcodes(expression.getType(), type); // widen
				for (int i = 0; i < cast.length; i++)
					visitor.visitInsn(cast[i]);
			}
			visitor.visitInsn(TypeUtils.getReturnOpcode(type));
		} else {
			visitor.visitInsn(Opcodes.RETURN);
		}
	}

	@Override
	public boolean canChangeFlow() {
		return true;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (expression == previous) {
			this.setExpression(newest);
			return;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public ReturnStmt copy() {
		return new ReturnStmt(type, expression == null ? null : expression.copy());
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ReturnStmt) {
			ReturnStmt ret = (ReturnStmt) s;
			return type.equals(ret.type) && expression.equivalent(ret.expression);
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return expression == null ? Collections.emptyList() : List.of(expression);
	}
}
