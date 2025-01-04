package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.IntegerRange;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class NewArrayExpr extends Expr {

	// TODO: Add validation
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
		this.setBounds(bounds);
		this.setType(type);
		this.setCst(cst);
	}

	public void setBounds(Expr[] bounds) {
		for (int i = 0; i < bounds.length; i++) {
			if (bounds[i] != null) bounds[i].unlink();
		}

		this.bounds = bounds;
		for (Expr bound : bounds) {
			bound.setParent(this);
		}
	}

	public void setCst(Expr[] cst) {
		for (int i = 0; i < cst.length; i++) {
			if (cst[i] != null) {
				cst[i].unlink();
			}
		}

		this.cst = cst;
		for (Expr expr : cst) {
			expr.setParent(this);
		}
	}

	public int getDimensions() {
		return bounds.length;
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

	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
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
			//System.out.println("Type: " + type.getInternalName() + " Element: " + type.getElementType().getInternalName() + " Csts: " + Arrays.deepToString(cst));
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
				case Type.SHORT: {
					for (int i = 0; i < cst.length; i++) {
						visitor.visitInsn(Opcodes.DUP);
						ConstantExpr.packInt(visitor, i);
						cst[i].toCode(visitor, assembler);
						visitor.visitInsn(Opcodes.SASTORE);
					}
					break;
				}
				case Type.CHAR: {
					for (int i = 0; i < cst.length; i++) {
						visitor.visitInsn(Opcodes.DUP);
						ConstantExpr.packInt(visitor, i);
						cst[i].toCode(visitor, assembler);
						visitor.visitInsn(Opcodes.CASTORE);
					}
					break;
				}
				case Type.FLOAT: {
					for (int i = 0; i < cst.length; i++) {
						visitor.visitInsn(Opcodes.DUP);
						ConstantExpr.packInt(visitor, i);
						cst[i].toCode(visitor, assembler);
						visitor.visitInsn(Opcodes.FASTORE);
					}
					break;
				}
				case Type.DOUBLE: {
					for (int i = 0; i < cst.length; i++) {
						visitor.visitInsn(Opcodes.DUP);
						ConstantExpr.packInt(visitor, i);
						cst[i].toCode(visitor, assembler);
						visitor.visitInsn(Opcodes.DASTORE);
					}
					break;
				}
				case Type.INT: {
					for (int i = 0; i < cst.length; i++) {
						visitor.visitInsn(Opcodes.DUP);
						ConstantExpr.packInt(visitor, i);
						cst[i].toCode(visitor, assembler);
						visitor.visitInsn(Opcodes.IASTORE);
					}
					break;
				}
				default:
					throw new IllegalStateException("Not implemented (type: " + type.getInternalName() + " elem: " + type.getElementType() + " csts: " + Arrays.deepToString(cst) + ")");
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
				bounds[i].unlink();
				bounds[i] = newest;
				bounds[i].setParent(this);
				return;
			}
		}

		for (int i = 0; i < cst.length; i++) {
			if (cst[i] == previous) {
				cst[i].unlink();
				cst[i] = newest;
				cst[i].setParent(this);
				return;
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

	@Override
	public List<CodeUnit> children() {
		final List<CodeUnit> self = new ArrayList<>();

        Collections.addAll(self, bounds);
        Collections.addAll(self, cst);

		return Collections.unmodifiableList(self);
	}
}