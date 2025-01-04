package org.mapleir.ir.code.expr;

import static org.objectweb.asm.Opcodes.*;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;

import java.util.List;

@Getter @Setter
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
	private ValueComparisonType comparisonType;

	public ComparisonExpr(Expr left, Expr right, ValueComparisonType type) {
		super(COMPARE);
		this.comparisonType = type;
		this.setLeft(left);
		this.setRight(right);
	}

	public void setLeft(Expr left) {
		if (this.left != null) {
			this.left.unlink();
		}

		this.left = left;
		this.left.setParent(this);
	}

	public void setRight(Expr right) {
		if (this.right != null) {
			this.right.unlink();
		}

		this.right = right;
		this.right.setParent(this);
	}

	@Override
	public Expr copy() {
		return new ComparisonExpr(left.copy(), right.copy(), comparisonType);
	}

	@Override
	public Type getType() {
		return Type.INT_TYPE;
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
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
			visitor.visitInsn(comparisonType == ValueComparisonType.GT ? Opcodes.FCMPG : Opcodes.FCMPL);
		} else if (left.getType() == Type.DOUBLE_TYPE || right.getType() == Type.DOUBLE_TYPE) {
			visitor.visitInsn(comparisonType == ValueComparisonType.GT ? Opcodes.DCMPG : Opcodes.DCMPL);
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
			this.setLeft(newest);
		} else if (right == previous) {
			this.setRight(newest);
		} else {
			throw new IllegalArgumentException(String.format(
					"Cannot overwrite %s with %s in %s",
					previous, newest, this
			));
		}
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ComparisonExpr) {
			ComparisonExpr comp = (ComparisonExpr) s;
			return comparisonType == comp.comparisonType && left.equivalent(comp.left) && right.equals(comp.right);
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(left, right);
	}
}