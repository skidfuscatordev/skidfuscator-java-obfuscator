package org.mapleir.ir.code;

import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;

public class FakeStmt extends Stmt {
	
	public FakeStmt() {
		super(0x2000);
	}
	
	public FakeStmt(FakeStmt other) {
		this();
		
		for(int i=0; i < other.size(); i++) {
			writeAt(other.read(i), i);
		}
	}

	@Override
	public Stmt copy() {
		return new FakeStmt();
	}

	@Override
	public void onChildUpdated(int ptr) {
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		throw new UnsupportedOperationException();
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
