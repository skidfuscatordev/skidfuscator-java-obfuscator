package org.mapleir.ir.code.stmt;

import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;

public class ConditionalJumpStmt extends Stmt {

	public enum ComparisonType {
		EQ("=="), NE("!="), LT("<"), GE(">="), GT(">"), LE("<="), ;

		
		private final String sign;

		private ComparisonType(String sign) {
			this.sign = sign;
		}

		public String getSign() {
			return sign;
		}

		public static ComparisonType getType(int opcode) {
			switch (opcode) {
				case Opcodes.IF_ACMPEQ:
				case Opcodes.IF_ICMPEQ:
				case Opcodes.IFEQ:
					return EQ;
				case Opcodes.IF_ACMPNE:
				case Opcodes.IF_ICMPNE:
				case Opcodes.IFNE:
					return NE;
				case Opcodes.IF_ICMPGT:
				case Opcodes.IFGT:
					return GT;
				case Opcodes.IF_ICMPGE:
				case Opcodes.IFGE:
					return GE;
				case Opcodes.IF_ICMPLT:
				case Opcodes.IFLT:
					return LT;
				case Opcodes.IF_ICMPLE:
				case Opcodes.IFLE:
					return LE;
				default:
					throw new IllegalArgumentException(Printer.OPCODES[opcode]);
			}
		}
	}

	private Expr left;
	private Expr right;
	private BasicBlock trueSuccessor;
	private ComparisonType type;
	private ConditionalJumpEdge<BasicBlock> edge;

	public ConditionalJumpStmt(Expr left, Expr right, BasicBlock trueSuccessor, ComparisonType type) {
		super(COND_JUMP);
		setLeft(left);
		setRight(right);
		setTrueSuccessor(trueSuccessor);
		setType(type);
	}

	public ConditionalJumpStmt(Expr left, Expr right, BasicBlock trueSuccessor, ComparisonType type, ConditionalJumpEdge<BasicBlock> edge) {
		super(COND_JUMP);
		setLeft(left);
		setRight(right);
		setTrueSuccessor(trueSuccessor);
		setType(type);
		setEdge(edge);
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

	public BasicBlock getTrueSuccessor() {
		return trueSuccessor;
	}

	public void setTrueSuccessor(BasicBlock trueSuccessor) {
		this.trueSuccessor = trueSuccessor;
	}

	public ComparisonType getComparisonType() {
		return type;
	}

	public void setType(ComparisonType type) {
		this.type = type;
	}

	public ConditionalJumpEdge<BasicBlock> getEdge() {
		return edge;
	}

	public void setEdge(ConditionalJumpEdge<BasicBlock> edge) {
		this.edge = edge;
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
	public void toString(TabbedStringWriter printer) {
		printer.print("if ");
		printer.print('(');
		left.toString(printer);
		printer.print(" " + type.getSign() + " ");
		right.toString(printer);
		printer.print(')');
		printer.tab();
		printer.print("\ngoto " + trueSuccessor.getDisplayName());
		printer.untab();
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		Type opType = TypeUtils.resolveBinOpType(left.getType(), right.getType());

		if (TypeUtils.isObjectRef(opType)) {
			boolean isNull = right instanceof ConstantExpr && ((ConstantExpr) right).getConstant() == null;
			if (type != ComparisonType.EQ && type != ComparisonType.NE) {
				throw new IllegalArgumentException(type.toString());
			}

			left.toCode(visitor, assembler);
			if (isNull) {
				visitor.visitJumpInsn(type == ComparisonType.EQ ? Opcodes.IFNULL : Opcodes.IFNONNULL, assembler.getLabel(trueSuccessor));
			} else {
				right.toCode(visitor, assembler);
				visitor.visitJumpInsn(type == ComparisonType.EQ ? Opcodes.IF_ACMPEQ : Opcodes.IF_ACMPNE, assembler.getLabel(trueSuccessor));
			}
		} else if (opType == Type.INT_TYPE) {
			boolean canShorten = right instanceof ConstantExpr
					&& ((ConstantExpr) right).getConstant() instanceof Number
					&& ((Number) ((ConstantExpr) right).getConstant()).intValue() == 0;

			left.toCode(visitor, assembler);
			int[] cast = TypeUtils.getPrimitiveCastOpcodes(left.getType(), opType);
			for (int i = 0; i < cast.length; i++) {
				visitor.visitInsn(cast[i]);
			}
			if (canShorten) {
				visitor.visitJumpInsn(Opcodes.IFEQ + type.ordinal(), assembler.getLabel(trueSuccessor));
			} else {
				right.toCode(visitor, assembler);
				cast = TypeUtils.getPrimitiveCastOpcodes(right.getType(), opType);
				for (int i = 0; i < cast.length; i++) {
					visitor.visitInsn(cast[i]);
				}
				visitor.visitJumpInsn(Opcodes.IF_ICMPEQ + type.ordinal(), assembler.getLabel(trueSuccessor));
			}
		} else if (opType == Type.LONG_TYPE) {
			left.toCode(visitor, assembler);
			int[] cast = TypeUtils.getPrimitiveCastOpcodes(left.getType(), opType);
			for (int i = 0; i < cast.length; i++) {
				visitor.visitInsn(cast[i]);
			}
			right.toCode(visitor, assembler);
			cast = TypeUtils.getPrimitiveCastOpcodes(right.getType(), opType);
			for (int i = 0; i < cast.length; i++) {
				visitor.visitInsn(cast[i]);
			}
			visitor.visitInsn(Opcodes.LCMP);
			visitor.visitJumpInsn(Opcodes.IFEQ + type.ordinal(), assembler.getLabel(trueSuccessor));
		} else if (opType == Type.FLOAT_TYPE) {
			left.toCode(visitor, assembler);
			int[] cast = TypeUtils.getPrimitiveCastOpcodes(left.getType(), opType);
			for (int i = 0; i < cast.length; i++) {
				visitor.visitInsn(cast[i]);
			}
			right.toCode(visitor, assembler);
			cast = TypeUtils.getPrimitiveCastOpcodes(right.getType(), opType);
			for (int i = 0; i < cast.length; i++) {
				visitor.visitInsn(cast[i]);
			}
			visitor.visitInsn((type == ComparisonType.LT || type == ComparisonType.LE) ? Opcodes.FCMPL : Opcodes.FCMPG);
			visitor.visitJumpInsn(Opcodes.IFEQ + type.ordinal(), assembler.getLabel(trueSuccessor));
		} else if (opType == Type.DOUBLE_TYPE) {
			left.toCode(visitor, assembler);
			int[] cast = TypeUtils.getPrimitiveCastOpcodes(left.getType(), opType);
			for (int i = 0; i < cast.length; i++) {
				visitor.visitInsn(cast[i]);
			}
			right.toCode(visitor, assembler);
			cast = TypeUtils.getPrimitiveCastOpcodes(right.getType(), opType);
			for (int i = 0; i < cast.length; i++) {
				visitor.visitInsn(cast[i]);
			}
			visitor.visitInsn((type == ComparisonType.LT || type == ComparisonType.LE) ? Opcodes.DCMPL : Opcodes.DCMPG);
			visitor.visitJumpInsn(Opcodes.IFEQ + type.ordinal(), assembler.getLabel(trueSuccessor));
		} else {
			throw new IllegalArgumentException(opType.toString());
		}
	}

	public int toOpcode() {
		Type opType = TypeUtils.resolveBinOpType(left.getType(), right.getType());

		if (TypeUtils.isObjectRef(opType)) {
			boolean isNull = right instanceof ConstantExpr && ((ConstantExpr) right).getConstant() == null;
			if (type != ComparisonType.EQ && type != ComparisonType.NE) {
				throw new IllegalArgumentException(type.toString());
			}

			if (isNull) {
				return type == ComparisonType.EQ ? Opcodes.IFNULL : Opcodes.IFNONNULL;
			} else {
				return type == ComparisonType.EQ ? Opcodes.IF_ACMPEQ : Opcodes.IF_ACMPNE;
			}
		} else if (opType == Type.INT_TYPE) {
			boolean canShorten = right instanceof ConstantExpr
					&& ((ConstantExpr) right).getConstant() instanceof Number
					&& ((Number) ((ConstantExpr) right).getConstant()).intValue() == 0;

			if (canShorten) {
				return Opcodes.IFEQ + type.ordinal();
			} else {
				return Opcodes.IF_ICMPEQ + type.ordinal();
			}
		} else if (opType == Type.LONG_TYPE) {
			return Opcodes.IFEQ + type.ordinal();
		} else if (opType == Type.FLOAT_TYPE) {
			return Opcodes.IFEQ + type.ordinal();
		} else if (opType == Type.DOUBLE_TYPE) {
			return Opcodes.IFEQ + type.ordinal();
		} else {
			throw new IllegalArgumentException(opType.toString());
		}
	}

	@Override
	public boolean canChangeFlow() {
		return true;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (previous == left) {
			left = newest;
			return;
		}

		if (previous == right) {
			right = newest;
			return;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public ConditionalJumpStmt copy() {
		return new ConditionalJumpStmt(left.copy(), right.copy(), trueSuccessor, type);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ConditionalJumpStmt) {
			ConditionalJumpStmt jump = (ConditionalJumpStmt) s;
			return type == jump.type && left.equivalent(jump.left) && right.equals(jump.right) && trueSuccessor == jump.trueSuccessor;
		}
		return false;
	}
}