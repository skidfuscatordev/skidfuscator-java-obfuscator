package org.mapleir.ir.code.expr;

import static org.objectweb.asm.Opcodes.*;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;

public class ComparisonExpr extends Expr {

	public enum ValueComparisonType {
		LT, GT, CMP;
		
		public static ValueComparisonType resolve(int opcode) {
			if(opcode == LCMP) {
				return CMP;
			} else if(opcode == FCMPG || opcode == DCMPG) {
				return ValueComparisonType.GT;
			} else if(opcode == FCMPL || opcode == DCMPL) {
				return ValueComparisonType.LT;
			} else {
				throw new UnsupportedOperationException(Printer.OPCODES[opcode]);
			}
		}
		
		public static Type resolveType(int opcode) {
			if(opcode == LCMP) {
				return Type.LONG_TYPE;
			} else if(opcode == FCMPG || opcode == FCMPL) {
				return Type.FLOAT_TYPE;
			} else if(opcode == DCMPG || opcode == DCMPL) {
				return Type.DOUBLE_TYPE;
			} else {
				throw new UnsupportedOperationException(Printer.OPCODES[opcode]);
			}
		}
	}

	private Expr left;
	private Expr right;
	private ValueComparisonType type;

	public ComparisonExpr(Expr left, Expr right, ValueComparisonType type) {
		super(COMPARE);
		this.type = type;
		setLeft(left);
		setRight(right);
	}

	public Expr getLeft() {
		return left;
	}

	public void setLeft(Expr left) {
		writeAt(left, 0);
	}

	public Expr getRight() {
		return right;
	}

	public void setRight(Expr right) {
		writeAt(right, 1);
	}

	public void setType(ValueComparisonType type) {
		this.type = type;
	}

	@Override
	public Expr copy() {
		return new ComparisonExpr(left.copy(), right.copy(), type);
	}

	@Override
	public Type getType() {
		return Type.INT_TYPE;
	}

	@Override
	public void onChildUpdated(int ptr) {
		if (ptr == 0) {
			left = read(0);
		} else if (ptr == 1) {
			right = read(1);
		} else {
			raiseChildOutOfBounds(ptr);
		}
	}
	
	@Override
	public Precedence getPrecedence0() {
		return Precedence.METHOD_INVOCATION;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print('(');
		left.toString(printer);
		printer.print(" CMP ");
		right.toString(printer);
		printer.print(")");
//		printer.print("? 0 : (");
//		right.toString(printer);
//		printer.print(" > ");
//		left.toString(printer);
//		printer.print("? 1 : -1))");
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		left.toCode(visitor, assembler);
		right.toCode(visitor, assembler);

		if (left.getType() == Type.LONG_TYPE || right.getType() == Type.LONG_TYPE) {
			visitor.visitInsn(Opcodes.LCMP);
		} else if (left.getType() == Type.FLOAT_TYPE || right.getType() == Type.FLOAT_TYPE) {
			visitor.visitInsn(type == ValueComparisonType.GT ? Opcodes.FCMPG : Opcodes.FCMPL);
		} else if (left.getType() == Type.DOUBLE_TYPE || right.getType() == Type.DOUBLE_TYPE) {
			visitor.visitInsn(type == ValueComparisonType.GT ? Opcodes.DCMPG : Opcodes.DCMPL);
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (left == previous) {
			left = newest;
		} else if (right == previous) {
			right = newest;
		}

		super.overwrite(previous, newest);
	}

	public ValueComparisonType getComparisonType() {
		return type;
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ComparisonExpr) {
			ComparisonExpr comp = (ComparisonExpr) s;
			return type == comp.type && left.equivalent(comp.left) && right.equals(comp.right);
		}
		return false;
	}
}