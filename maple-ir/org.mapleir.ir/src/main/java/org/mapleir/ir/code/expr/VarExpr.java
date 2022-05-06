package org.mapleir.ir.code.expr;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.ir.locals.Local;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class VarExpr extends Expr {

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
	
	public Local getLocal() {
		return local;
	}
	
	public void setLocal(Local local) {
		this.local = local;
	}

	@Override
	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public VarExpr copy() {
		return new VarExpr(local, type);
	}

	@Override
	public void onChildUpdated(int ptr) {
		raiseChildOutOfBounds(ptr);
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
