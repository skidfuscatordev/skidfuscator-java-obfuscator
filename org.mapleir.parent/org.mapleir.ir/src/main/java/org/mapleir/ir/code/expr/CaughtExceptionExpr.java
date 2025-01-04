package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class CaughtExceptionExpr extends Expr {

	private Type type;
	
	public CaughtExceptionExpr(Type type) {
		super(CATCH);
		this.setType(type);
	}

	public CaughtExceptionExpr(String type) {
		super(CATCH);
		if (type == null) {
			type = "Ljava/lang/Throwable;";
		} else {
			type = "L" + type + ";";
		}
		this.type = Type.getType(type);
	}

	@Override
	public Expr copy() {
		return new CaughtExceptionExpr(type);
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("catch()");
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		throw new IllegalArgumentException(String.format(
				"Cannot overwrite %s with %s in %s",
				previous, newest, this
		));
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s.getOpcode() == CATCH) {
			CaughtExceptionExpr e = (CaughtExceptionExpr) s;
			return type.equals(e.type);
		} else {
			return false;
		}
	}
}