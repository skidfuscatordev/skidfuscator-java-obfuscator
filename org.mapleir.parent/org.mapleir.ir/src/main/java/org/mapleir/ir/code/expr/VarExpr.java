package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.ir.locals.Local;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class VarExpr extends Expr {
	// TODO: Add validation
	private Local local;
	private Type type;
	
	public VarExpr(Local local, Type type) {
		super(LOCAL_LOAD);
		this.local = local;
		this.type = type;
	}

	public int getIndex() {
		return local.getIndex();
	}
	
	@Override
	public Type getType() {
		return type;
	}
	
	@Override
	public VarExpr copy() {
		return new VarExpr(local, type);
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
//		printer.print("(" + type + ")" + local.toString());
		printer.print(local.toString());
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		if(local.isStoredInLocal()) {
			visitor.visitVarInsn(TypeUtils.getVariableLoadOpcode(getType()), local.getCodeIndex());	
		}
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}
	
	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof VarExpr) {
			VarExpr var = (VarExpr) s;
			if (type == null) // fix for incomplete type analysis
				return false;
			return local == var.local && type.equals(var.type);
		}
		return false;
	}
}
