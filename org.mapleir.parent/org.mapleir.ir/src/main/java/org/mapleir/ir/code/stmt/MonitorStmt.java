package org.mapleir.ir.code.stmt;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

@Getter @Setter
public class MonitorStmt extends Stmt {

	public enum MonitorMode {
		ENTER, EXIT;
	}

	private Expr expression;
	private MonitorMode mode;

	public MonitorStmt(Expr expression, MonitorMode mode) {
		super(MONITOR);
		this.mode = mode;
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
		printer.print(mode == MonitorMode.ENTER ? "MONITORENTER" : "MONITOREXIT");
		printer.print('(');
		expression.toString(printer);
		printer.print(')');
		printer.print(';');		
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		expression.toCode(visitor, assembler);
		visitor.visitInsn(mode == MonitorMode.ENTER ? Opcodes.MONITORENTER : Opcodes.MONITOREXIT);		
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (expression.equivalent(previous)) {
			this.setExpression(newest);
			return;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public MonitorStmt copy() {
		return new MonitorStmt(expression.copy(), mode);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof MonitorStmt) {
			MonitorStmt mon = (MonitorStmt) s;
			return mode == mon.mode && expression.equivalent(mon.expression);
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(expression);
	}
}