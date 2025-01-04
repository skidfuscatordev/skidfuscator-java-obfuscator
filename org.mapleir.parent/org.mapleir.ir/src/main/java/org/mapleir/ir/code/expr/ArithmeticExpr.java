package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

 @Getter @Setter
public class ArithmeticExpr extends Expr {

	public enum Operator {
		// TODO: verify bitwise order
		ADD("+", false), SUB("-", true), MUL("*", false), DIV("/", true), REM("%", true), SHL("<<", true), SHR(">>", true), USHR(">>>", true), OR("|", true), AND("&", true), XOR("^", true);
		private final String sign;
		private final boolean order;

		private Operator(String sign, boolean order) {
			this.sign = sign;
			this.order = order;
		}

		public String getSign() {
			return sign;
		}
		
		public boolean doesOrderMatter() {
			return order;
		}
		
		public static Operator resolve(int bOpcode) {
			if(bOpcode >= IADD && bOpcode <= DREM){
				return values()[(int)Math.floor((bOpcode - IADD) / 4) + Operator.ADD.ordinal()];
			} else if(bOpcode >= ISHL && bOpcode <= LUSHR) {
				return values()[(int)Math.floor((bOpcode - ISHL) / 2) + Operator.SHL.ordinal()];
			} else if(bOpcode == IAND || bOpcode == LAND) {
				return Operator.AND;
			} else if(bOpcode == IOR || bOpcode == LOR) {
				return Operator.OR;
			} else if(bOpcode == IXOR || bOpcode == LXOR) {
				return Operator.XOR;
			} else {
				throw new UnsupportedOperationException(Printer.OPCODES[bOpcode]);
			}
		}
		
		public static Type resolveType(int bOpcode) {
			switch(bOpcode) {
				case IADD:
				case ISUB:
				case IMUL:
				case IDIV:
				case IREM:
				case INEG:
				case ISHL:
				case ISHR:
				case IUSHR:
				case IAND:
				case IOR:
				case IXOR:
					return Type.INT_TYPE;
				case LADD:
				case LSUB:
				case LMUL:
				case LDIV:
				case LREM:
				case LNEG:
				case LSHL:
				case LSHR:
				case LUSHR:
				case LAND:
				case LOR:
				case LXOR:
					return Type.LONG_TYPE;
				case FADD:
				case FSUB:
				case FMUL:
				case FDIV:
				case FREM:
				case FNEG:
					return Type.FLOAT_TYPE;
				case DADD:
				case DSUB:
				case DMUL:
				case DDIV:
				case DREM:
				case DNEG:
					return Type.DOUBLE_TYPE;
					
				default:
					throw new UnsupportedOperationException(Printer.OPCODES[bOpcode]);
			}
		}
	}

	private Expr right;
	private Expr left;
	private Operator operator;

	// TODO: arg order...
	public ArithmeticExpr(Expr right, Expr left, Operator operator) {
		super(ARITHMETIC);
		this.operator = operator;
		this.setLeft(left);
		this.setRight(right);
	}

	public void setLeft(Expr left) {
		if (this.left != null)
			this.left.unlink();

		this.left = left;
		this.left.setParent(this);
	}

	public void setRight(Expr right) {
		if (this.right != null)
			this.right.unlink();

		this.right = right;
		this.right.setParent(this);
	}

	@Override
	public Expr copy() {
		return new ArithmeticExpr(right.copy(), left.copy(), operator);
	}

	@Override
	public Type getType() {
		if (operator == Operator.SHL || operator == Operator.SHR) {
			return TypeUtils.resolveUnaryOpType(left.getType());
		} else {
			try {
				return TypeUtils.resolveBinOpType(left.getType(), right.getType());
			} catch (IllegalStateException e) {
				throw new IllegalStateException(
						"Failed type merge: " + left + " vs " + right
						+ " (typeLeft: " + left.getType() +  " typeRight: " + right.getType() + ") op " + operator
								+ " block: " + getBlock(), e
				);
			}
		}
	}

	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public Precedence getPrecedence0() {
		switch (operator) {
			case ADD:
			case SUB:
				return Precedence.ADD_SUB;
			case MUL:
			case DIV:
			case REM:
				return Precedence.MUL_DIV_REM;
			case SHL:
			case SHR:
			case USHR:
				return Precedence.BITSHIFT;
			case OR:
				return Precedence.BIT_OR;
			case AND:
				return Precedence.BIT_AND;
			case XOR:
				return Precedence.BIT_XOR;
			default:
				return super.getPrecedence0();
		}
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("{");
		int selfPriority = getPrecedence();
		int leftPriority = left.getPrecedence();
		int rightPriority = right.getPrecedence();
		if (leftPriority > selfPriority)
			printer.print('(');
		left.toString(printer);
		if (leftPriority > selfPriority)
			printer.print(')');
		printer.print(" " + operator.getSign() + " ");
		if (rightPriority > selfPriority)
			printer.print('(');
		right.toString(printer);
		if (rightPriority > selfPriority)
			printer.print(')');
		printer.print("}");
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		Type leftType = null;
		Type rightType = null;
		if (operator == Operator.SHL || operator == Operator.SHR || operator == Operator.USHR) {
			leftType = getType();
			rightType = Type.INT_TYPE;
		} else {
			leftType = rightType = getType();
		}
		left.toCode(visitor, assembler);
		int[] lCast = TypeUtils.getPrimitiveCastOpcodes(left.getType(), leftType);
		for (int i = 0; i < lCast.length; i++)
			visitor.visitInsn(lCast[i]);

		right.toCode(visitor, assembler);
		int[] rCast = TypeUtils.getPrimitiveCastOpcodes(right.getType(), rightType);
		for (int i = 0; i < rCast.length; i++)
			visitor.visitInsn(rCast[i]);
		int opcode;
		switch (operator) {
			case ADD:
				opcode = TypeUtils.getAddOpcode(getType());
				break;
			case SUB:
				opcode = TypeUtils.getSubtractOpcode(getType());
				break;
			case MUL:
				opcode = TypeUtils.getMultiplyOpcode(getType());
				break;
			case DIV:
				opcode = TypeUtils.getDivideOpcode(getType());
				break;
			case REM:
				opcode = TypeUtils.getRemainderOpcode(getType());
				break;
			case SHL:
				opcode = TypeUtils.getBitShiftLeftOpcode(getType());
				break;
			case SHR:
				opcode = TypeUtils.bitShiftRightOpcode(getType());
				break;
			case USHR:
				opcode = TypeUtils.getBitShiftRightUnsignedOpcode(getType());
				break;
			case OR:
				opcode = TypeUtils.getBitOrOpcode(getType());
				break;
			case AND:
				opcode = TypeUtils.getBitAndOpcode(getType());
				break;
			case XOR:
				opcode = TypeUtils.getBitXorOpcode(getType());
				break;
			default:
				throw new RuntimeException();
		}
		visitor.visitInsn(opcode);
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ArithmeticExpr) {
			ArithmeticExpr arith = (ArithmeticExpr) s;
			return arith.operator == operator && left.equivalent(arith.left) && right.equivalent(arith.right);
		}
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

//	@Override
//	public int getExecutionCost() {
//		// TODO Auto-generated method stub
//		return 0;
//	}


	@Override
	public List<CodeUnit> children() {
		return List.of(left, right);
	}
}