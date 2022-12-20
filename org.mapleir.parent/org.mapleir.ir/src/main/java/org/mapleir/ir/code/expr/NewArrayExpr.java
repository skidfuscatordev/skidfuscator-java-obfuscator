package org.mapleir.ir.code.expr;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.IntegerRange;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;

public class NewArrayExpr extends Expr {

	private Expr[] bounds;
	private Type type;

	private Expr[] cst;

	public NewArrayExpr(Expr[] bounds, Type type) {
		this(bounds, type, new Expr[0]);
//		if(type.getSort() == Type.ARRAY) {
//			throw new RuntimeException(type.toString());
//		}
	}

	public NewArrayExpr(Expr[] bounds, Type type, Expr[] cst) {
		super(NEW_ARRAY);
		this.bounds = bounds;
		this.type = type;
		this.cst = cst;

		for (int i = 0; i < bounds.length; i++) {
			writeAt(bounds[i], i);
		}

		for (int i = 0; i < cst.length; i++) {
			writeAt(cst[i], bounds.length + i);
		}
	}

	public int getDimensions() {
		return bounds.length;
	}
	
	public Expr[] getBounds() {
		return bounds;
	}

	public Expr[] getCst() {
		return cst;
	}

	public void setCst(Expr[] cst) {
		this.cst = cst;

		for (int i = 0; i < cst.length; i++) {
			writeAt(cst[i], bounds.length + i);
		}
	}

	@Override
	public Expr copy() {
		Expr[] bounds = new Expr[this.bounds.length];
		for (int i = 0; i < bounds.length; i++)
			bounds[i] = this.bounds[i].copy();

		Expr[] cst = new Expr[this.cst.length];
		for (int i = 0; i < cst.length; i++)
			cst[i] = this.cst[i].copy();

		return new NewArrayExpr(bounds, type, cst);
	}

	@Override
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public void onChildUpdated(int ptr) {
		if(ptr >= 0 && ptr < bounds.length) {
			bounds[ptr] = read(ptr);
		} else if (ptr >= bounds.length && ptr < bounds.length + cst.length) {
			cst[ptr - bounds.length] = read(ptr);
		} else {
			raiseChildOutOfBounds(ptr);
		}
	}
	
	@Override
	public Precedence getPrecedence0() {
		return Precedence.ARRAY_ACCESS;
	}

	// TODO: redo type to element type.
	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("new " + type.getElementType().getClassName());
		for (int dim = 0; dim < type.getDimensions(); dim++) {
			printer.print('[');
			if (dim < bounds.length) {
				bounds[dim].toString(printer);
			}
			printer.print(']');
		}
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		for (int i = 0; i < bounds.length; i++) {
			bounds[i].toCode(visitor, assembler);
			int[] cast = TypeUtils.getPrimitiveCastOpcodes(bounds[i].getType(), Type.INT_TYPE);
			for (int a = 0; a < cast.length; a++)
				visitor.visitInsn(cast[a]);
		}

		if (type.getDimensions() != 1) {
			visitor.visitMultiANewArrayInsn(type.getDescriptor(), bounds.length);
		} else {
			Type element = type.getElementType();
			if (element.getSort() == Type.OBJECT || element.getSort() == Type.METHOD) {
				visitor.visitTypeInsn(Opcodes.ANEWARRAY, element.getInternalName());
			} else {
				visitor.visitIntInsn(Opcodes.NEWARRAY, TypeUtils.getPrimitiveArrayOpcode(type));
			}
		}

		if (cst.length > 0 && type.getDimensions() != 1) {
			throw new IllegalStateException("Not implemented");
		}

		if (cst.length > 0) {
			switch (type.getElementType().getSort()) {
				case Type.BYTE: {
					for (int i = 0; i < cst.length; i++) {
						visitor.visitInsn(Opcodes.DUP);
						ConstantExpr.packInt(visitor, i);
						cst[i].toCode(visitor, assembler);
						visitor.visitInsn(Opcodes.BASTORE);
					}
					break;
				}
				default:
					throw new IllegalStateException("Not implemented (type: " + type.getInternalName() + " csts: " + Arrays.deepToString(cst) + ")");
			}
		}
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		for (int i = 0; i < bounds.length; i++) {
			if (bounds[i] == previous) {
				bounds[i] = newest;
				break;
			}
		}

		super.overwrite(previous, newest);
	}
	
	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof NewArrayExpr) {
			NewArrayExpr e = (NewArrayExpr) s;
			if(e.bounds.length != bounds.length) {
				return false;
			}
			for(int i=0; i < bounds.length; i++) {
				if(!bounds[i].equivalent(e.bounds[i])) {
					return false;
				}
			}
			return type.equals(e.type);
		}
		return false;
	}
}