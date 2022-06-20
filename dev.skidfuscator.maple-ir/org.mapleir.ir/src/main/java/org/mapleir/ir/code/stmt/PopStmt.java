package org.mapleir.ir.code.stmt;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class PopStmt extends Stmt {

	private Expr expression;
	
	public PopStmt(Expr expression) {
		super(POP);
		setExpression(expression);
	}

	public Expr getExpression() {
		return expression;
	}

	public void setExpression(Expr expression) {
		writeAt(expression, 0);
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
			expression = newest;
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
}