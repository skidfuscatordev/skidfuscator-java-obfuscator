package org.mapleir.ir.code;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class FakeExpr extends Expr {

	public FakeExpr() {
		super(0x4000);
	}
	
	public FakeExpr(FakeExpr other) {
		this();
		
		for(int i=0; i < other.size(); i++) {
			writeAt(other.read(i), i);
		}
	}

	@Override
	public void onChildUpdated(int ptr) {
		
	}

	@Override
	public Expr copy() {
		return new FakeExpr(this);
	}

	@Override
	public Type getType() {
		return TypeUtils.OBJECT_TYPE;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("<" + toString() + ">");
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		return false;
	}
	
	@Override
	public String toString() {
		return getDisplayName();
	}
}
