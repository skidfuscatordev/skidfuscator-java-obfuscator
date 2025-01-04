package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;

@Getter @Setter
public class AllocObjectExpr extends Expr {
	// TODO: Add validation
	private Type type;

	public AllocObjectExpr(Type type) {
		super(ALLOC_OBJ);
		this.setType(type);
	}

	@Override
	public Expr copy() {
		return new AllocObjectExpr(type);
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public void onChildUpdated(int ptr) {
		raiseChildOutOfBounds(ptr);
	}
	
	@Override
	public Precedence getPrecedence0() {
		return Precedence.NEW;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("new " + type.getClassName());		
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		visitor.visitTypeInsn(Opcodes.NEW, type.getInternalName());		
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		throw new IllegalArgumentException(String.format(
				"Cannot overwrite %s with %s in %s",
				previous, newest, this
		));
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		return s instanceof AllocObjectExpr && type.equals(((AllocObjectExpr) s).type);
	}
}