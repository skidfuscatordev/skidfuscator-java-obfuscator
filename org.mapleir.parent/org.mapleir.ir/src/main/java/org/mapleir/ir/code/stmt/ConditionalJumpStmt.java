package org.mapleir.ir.code.stmt;

import lombok.Getter;
import lombok.Setter;
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

import java.util.List;

@Getter @Setter
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
	private ComparisonType comparisonType;
	private ConditionalJumpEdge<BasicBlock> edge;

	public ConditionalJumpStmt(Expr left, Expr right, BasicBlock trueSuccessor, ComparisonType type) {
		super(COND_JUMP);
		this.setLeft(left);
		this.setRight(right);
		this.setTrueSuccessor(trueSuccessor);
		this.setComparisonType(type);
	}

	public ConditionalJumpStmt(Expr left, Expr right, BasicBlock trueSuccessor, ComparisonType type, ConditionalJumpEdge<BasicBlock> edge) {
		super(COND_JUMP);
		this.setLeft(left);
		this.setRight(right);
		this.setTrueSuccessor(trueSuccessor);
		this.setComparisonType(type);
		this.setEdge(edge);
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

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("if ");
		printer.print('(');
		left.toString(printer);
		printer.print(" " + comparisonType.getSign() + " ");
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
			if (comparisonType != ComparisonType.EQ && comparisonType != ComparisonType.NE) {
				throw new IllegalArgumentException(comparisonType.toString());
			}

			left.toCode(visitor, assembler);
			if (isNull) {
				visitor.visitJumpInsn(comparisonType == ComparisonType.EQ ? Opcodes.IFNULL : Opcodes.IFNONNULL, assembler.getLabel(trueSuccessor));
			} else {
				right.toCode(visitor, assembler);
				visitor.visitJumpInsn(comparisonType == ComparisonType.EQ ? Opcodes.IF_ACMPEQ : Opcodes.IF_ACMPNE, assembler.getLabel(trueSuccessor));
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
				visitor.visitJumpInsn(Opcodes.IFEQ + comparisonType.ordinal(), assembler.getLabel(trueSuccessor));
			} else {
				right.toCode(visitor, assembler);
				cast = TypeUtils.getPrimitiveCastOpcodes(right.getType(), opType);
				for (int i = 0; i < cast.length; i++) {
					visitor.visitInsn(cast[i]);
				}
				visitor.visitJumpInsn(Opcodes.IF_ICMPEQ + comparisonType.ordinal(), assembler.getLabel(trueSuccessor));
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
			visitor.visitJumpInsn(Opcodes.IFEQ + comparisonType.ordinal(), assembler.getLabel(trueSuccessor));
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
			visitor.visitInsn((comparisonType == ComparisonType.LT || comparisonType == ComparisonType.LE) ? Opcodes.FCMPL : Opcodes.FCMPG);
			visitor.visitJumpInsn(Opcodes.IFEQ + comparisonType.ordinal(), assembler.getLabel(trueSuccessor));
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
			visitor.visitInsn((comparisonType == ComparisonType.LT || comparisonType == ComparisonType.LE) ? Opcodes.DCMPL : Opcodes.DCMPG);
			visitor.visitJumpInsn(Opcodes.IFEQ + comparisonType.ordinal(), assembler.getLabel(trueSuccessor));
		} else {
			throw new IllegalArgumentException(opType.toString());
		}
	}

	public int toOpcode() {
		Type opType = TypeUtils.resolveBinOpType(left.getType(), right.getType());

		if (TypeUtils.isObjectRef(opType)) {
			boolean isNull = right instanceof ConstantExpr && ((ConstantExpr) right).getConstant() == null;
			if (comparisonType != ComparisonType.EQ && comparisonType != ComparisonType.NE) {
				throw new IllegalArgumentException(comparisonType.toString());
			}

			if (isNull) {
				return comparisonType == ComparisonType.EQ ? Opcodes.IFNULL : Opcodes.IFNONNULL;
			} else {
				return comparisonType == ComparisonType.EQ ? Opcodes.IF_ACMPEQ : Opcodes.IF_ACMPNE;
			}
		} else if (opType == Type.INT_TYPE) {
			boolean canShorten = right instanceof ConstantExpr
					&& ((ConstantExpr) right).getConstant() instanceof Number
					&& ((Number) ((ConstantExpr) right).getConstant()).intValue() == 0;

			if (canShorten) {
				return Opcodes.IFEQ + comparisonType.ordinal();
			} else {
				return Opcodes.IF_ICMPEQ + comparisonType.ordinal();
			}
		} else if (opType == Type.LONG_TYPE) {
			return Opcodes.IFEQ + comparisonType.ordinal();
		} else if (opType == Type.FLOAT_TYPE) {
			return Opcodes.IFEQ + comparisonType.ordinal();
		} else if (opType == Type.DOUBLE_TYPE) {
			return Opcodes.IFEQ + comparisonType.ordinal();
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
			this.setLeft(newest);
			return;
		}

		if (previous == right) {
			this.setRight(newest);
			return;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public ConditionalJumpStmt copy() {
		return new ConditionalJumpStmt(left.copy(), right.copy(), trueSuccessor, comparisonType);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ConditionalJumpStmt) {
			ConditionalJumpStmt jump = (ConditionalJumpStmt) s;
			return comparisonType == jump.comparisonType && left.equivalent(jump.left) && right.equals(jump.right) && trueSuccessor == jump.trueSuccessor;
		}
		return false;
	}

	@Override
	public List<CodeUnit> children() {
		return List.of(left, right);
	}
}