package org.mapleir.ir.code.stmt;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class NopStmt extends Stmt {
	public NopStmt() {
		super(NOP);
	}

	@Override
	public void onChildUpdated(int ptr) {
		raiseChildOutOfBounds(ptr);
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("nop;");
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		visitor.visitInsn(Opcodes.NOP);
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}
	
	@Override
	public NopStmt copy() {
		return new NopStmt();
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		return s instanceof NopStmt;
	}
}