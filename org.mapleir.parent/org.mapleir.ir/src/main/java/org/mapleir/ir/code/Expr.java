package org.mapleir.ir.code;

import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.NegationExpr;
import org.objectweb.asm.Type;

import java.util.Set;

public abstract class Expr extends CodeUnit {

	public enum Precedence {
		NORMAL,
		ARRAY_ACCESS,
		METHOD_INVOCATION,
		MEMBER_ACCESS,
		UNARY_PLUS_MINUS,
		PLUS_MIN_PREPOSTFIX,
		UNARY_LOGICAL_NOT,
		UNARY_BINARY_NOT,
		CAST,
		NEW,
		MUL_DIV_REM,
		ADD_SUB,
		STRING_CONCAT,
		BITSHIFT,
		LE_LT_GE_GT_INSTANCEOF,
		EQ_NE,
		BIT_AND,
		BIT_XOR,
		BIT_OR,
		LOGICAL_AND,
		LOGICAL_OR,
		TERNARY,
		ASSIGNMENT
	}
	
	protected CodeUnit parent;
	
	public Expr(int opcode) {
		super(opcode);
	}
	
	@Override
	public abstract void onChildUpdated(int ptr);
	
	@Override
	public abstract Expr copy();
	
	public abstract Type getType();
	
	public int getPrecedence() {
		return getPrecedence0().ordinal();
	}
	
	protected Precedence getPrecedence0() {
		return Precedence.NORMAL;
	}
	
	public CodeUnit getParent() {
		return parent;
	}

	public void hardUnlink() {
		if(parent != null) {
			//parent.overwrite(this, null);
			//System.out.append(String.format(
			//		"Unlinking %s from %s\n", this, parent
			//));
			parent.overwrite(this, null);
			//setParent(null);
			//parent.deleteAt(parent.indexOf(this));
		}
	}
	
	public void unlink() {
		if(parent != null) {
			//parent.overwrite(this, null);
			//System.out.append(String.format(
			//	"Unlinking %s from %s\n", this, parent
			//));
			setParent(null);
			//parent.deleteAt(parent.indexOf(this));
		}
	}
	
	public void setParent(CodeUnit parent) {
		if (this.parent != null && parent != null && this.parent != parent) {
			throw new IllegalStateException("Parent already set: " + this.parent);
		}

		this.parent = parent;
		if(parent != null) {
			setBlock(parent.getBlock());
		} else {
			setBlock(null);
		}
	}
	
	public Stmt getRootParent() {
		CodeUnit p = parent;
		if(p == null) {
			/* expressions must have a parent. */
			// except for phi args?
			//throw new UnsupportedOperationException("We've found a dangler, " + id + ". " + this);
			return null;
		} else {
			if((p.flags & FLAG_STMT) != 0) {
				return (Stmt) p;
			} else {
				return ((Expr) p).getRootParent();
			}
		}
	}

	// Manifold extension
	public Expr plus(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.ADD);
	}

	public Expr minus(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.SUB);
	}

	public Expr times(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.MUL);
	}

	public Expr div(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.DIV);
	}

	public Expr rem(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.REM);
	}

	public Expr and(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.AND);
	}

	public Expr or(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.OR);
	}

	public Expr xor(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.XOR);
	}

	public Expr ushr(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.USHR);
	}

	public Expr shl(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.SHL);
	}

	public Expr shr(Expr other) {
		return new ArithmeticExpr(other, this, ArithmeticExpr.Operator.SHR);
	}

	public Expr unaryMinus() {
		return new NegationExpr(this);
	}

	public Iterable<Expr> enumerateWithSelf() {
		Set<Expr> set = _enumerate();
		set.add(this);
		return set;
	}
	
	public static String typesToString(Expr[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i] == null ? "NULL" : a[i].getType());
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
	}
}
