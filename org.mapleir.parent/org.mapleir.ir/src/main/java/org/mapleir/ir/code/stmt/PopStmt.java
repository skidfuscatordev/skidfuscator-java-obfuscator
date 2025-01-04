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
import org.objectweb.asm.Type;

import java.util.List;

@Getter @Setter
public class PopStmt extends Stmt {

	private Expr expression;
	
	public PopStmt(Expr expression) {
		super(POP);
		this.setExpression(expression);
	}

	public void setExpression(Expr expression) {
		if (this.expression != null) {
			this.expression.unlink();
		}

		this.expression = expression;
		this.expression.setParent(this);
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("_consume(");
		if(expression != null) {
			expression.toString(printer);
		} else {
			printer.print("_NULL_STMT_");
		}
		printer.print(");");		
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		expression.toCode(visitor, assembler);
		if (expression.getType() != Type.VOID_TYPE)
			visitor.visitInsn(TypeUtils.getPopOpcode(expression.getType()));	
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
	public PopStmt copy() {
		return new PopStmt(expression.copy());
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		return s instanceof PopStmt && expression.equivalent(((PopStmt) s).expression);
	}
	@Override
	public List<CodeUnit> children() {
		return List.of(expression);
	}
}