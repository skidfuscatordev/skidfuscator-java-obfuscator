package org.mapleir.ir.code.stmt.copy;

import com.google.errorprone.annotations.Var;
import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.ir.locals.Local;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is the base for the two types of copy/move statements in the IR:
 * {@link CopyVarStmt} and {@link CopyPhiStmt}.<br>
 * All copy statements <b>must</b> have a local variable on the left hand(LHS)
 * of the statement and may have any expression, including complex expressions
 * and {@link PhiExpr}'s as their right hand sides(RHS).<br>
 * The general format of a copy statement is as follows:
 * <pre>var = rhsExpr</pre>
 * <p>The {@link VarExpr} for the destination/LHS of the copy is not entered
 * into the current unit's children array as it is technically not an
 * executable part of the statement.
 * <p>Additionally a copy statement may be considered 'synthetic' meaning that the
 * statement declaration is not useful in the sense that it affects the program
 * state and instead is used as a marker in the IR for easier analysis.
 * Specifically this is textually represented by having the LHS and RHS of the
 * copy as the same variable expression. i.e. <pre>synth(lvar0 = lvar0)</pre>
 * In this case, the same {@link VarExpr} is used (same object) and is not
 * entered into the children array as either the LHS or RHS as the entire
 * statement is never executed at runtime.
 */

@Getter @Setter
public abstract class AbstractCopyStmt extends Stmt {

	/**
	 * Whether or not this copy is a synthetic copy. See class javadocs.
	 */
	protected final boolean synthetic;
	/**
	 * The RHS expression (source).
	 */
	protected Expr expression;
	/**
	 * The LHS variable (destination).
	 */
	protected VarExpr variable;
	
	public AbstractCopyStmt(int opcode, VarExpr variable, Expr expression) {
		this(opcode, variable, expression, false);
	}
	
	public AbstractCopyStmt(int opcode, VarExpr variable, Expr expression, boolean synthetic) {
		super(opcode);
		
		if (variable == null | expression == null)
			throw new IllegalArgumentException("Neither variable nor statement can be null!");
		
		/* set these here because we only call writeAt if it's not a synthetic
		 * and if we don't call writeAt, the callback won't be invoked to set
		 * the expression field. */
		this.synthetic = synthetic;
		this.setExpression(expression);
		this.setVariable(variable);
	}

	public void setVariable(VarExpr var) {
		if (this.variable != null) {
			this.variable.unlink();
		}

		this.variable = var;
		if(synthetic) {
			this.expression = var;
		}

		var.setParent(this);
	}

	public void setExpression(Expr expression) {
		if (this.expression != null) {
			this.expression.unlink();
		}

		this.expression = expression;

		if (!synthetic && expression != null)
			this.expression.setParent(this);
	}

	public int getIndex() {
		return variable.getLocal().getIndex();
	}

	public Type getType() {
		return variable.getType();
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print(toString());
	}
	
	@Override
	public String toString() {
		if(synthetic) {
			return "synth(" + variable + " = " + expression + ");";
		} else {
			return variable + " = " + expression + ";";
		}
	}

	@Override
	// todo: this probably needs a refactoring
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		if(expression instanceof VarExpr) {
			if(((VarExpr) expression).getLocal() == variable.getLocal()) {
				return;
			}
		}
		
		variable.getLocal().setTempLocal(false);
		
		expression.toCode(visitor, assembler);
		Type type = variable.getType();
		if (TypeUtils.isPrimitive(type)) {
			int[] cast = TypeUtils.getPrimitiveCastOpcodes(expression.getType(), type);
			for (int i = 0; i < cast.length; i++)
				visitor.visitInsn(cast[i]);
		}

		Local local = variable.getLocal();
		if(local.isStack()) {
			visitor.visitVarInsn(TypeUtils.getVariableStoreOpcode(getType()), variable.getLocal().getCodeIndex());
			variable.getLocal().setTempLocal(true);
		} else {
			visitor.visitVarInsn(TypeUtils.getVariableStoreOpcode(getType()), variable.getLocal().getCodeIndex());
		}
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}
	
	public boolean isRedundant() {
		if(!synthetic && expression instanceof VarExpr) {
			return ((VarExpr) expression).getLocal() == variable.getLocal();
		} else {
			return false;
		}
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (previous == expression) {
			this.setExpression(newest);
			return;
		} else if (previous == variable && newest instanceof VarExpr varExpr) {
			this.setVariable(varExpr);
			return;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public abstract AbstractCopyStmt copy();

	@Override
	public abstract boolean equivalent(CodeUnit s);


	@Override
	public List<CodeUnit> linkedChildren() {
		return List.of(expression);
	}

	@Override
	public List<CodeUnit> children() {
		return synthetic || expression == null ? Collections.emptyList() : List.of(expression);
	}
}