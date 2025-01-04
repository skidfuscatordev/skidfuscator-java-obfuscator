package org.mapleir.ir.code.stmt;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class ThrowStmt extends Stmt {

	// TODO: Add validation
	@NonNull
	private Expr expression;

	public ThrowStmt(Expr expression) {
		super(THROW);
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
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("throw ");
		expression.toString(printer);
		printer.print(';');		
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		expression.toCode(visitor, assembler);
		visitor.visitInsn(Opcodes.ATHROW);		
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
	public ThrowStmt copy() {
		return new ThrowStmt(expression.copy());
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ThrowStmt) {
			ThrowStmt thr = (ThrowStmt) s;
			return expression.equivalent(thr.expression);
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(expression);
	}
}