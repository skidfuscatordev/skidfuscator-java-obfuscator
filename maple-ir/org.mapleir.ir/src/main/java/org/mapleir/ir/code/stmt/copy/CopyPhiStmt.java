package org.mapleir.ir.code.stmt.copy;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.code.expr.VarExpr;

public class CopyPhiStmt extends AbstractCopyStmt {

	public CopyPhiStmt(VarExpr variable, PhiExpr expression) {
		super(PHI_STORE, variable, expression);
	}

	public CopyPhiStmt(VarExpr variable, PhiExpr expression, boolean synthetic) {
		super(PHI_STORE, variable, expression, synthetic);
	}
	
	@Override
	public PhiExpr getExpression() {
		return (PhiExpr) super.getExpression();
	}
	
	@Override
	public void setExpression(Expr expression) {
		if(expression != null && !(expression instanceof PhiExpr)) {
			throw new UnsupportedOperationException(expression.toString());
		}
		
		super.setExpression(expression);
	}

	@Override
	public CopyPhiStmt copy() {
		return new CopyPhiStmt(getVariable().copy(), getExpression().copy(), isSynthetic());
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof CopyPhiStmt) {
			CopyPhiStmt copy = (CopyPhiStmt) s;
			return getExpression().equivalent(copy.getExpression()) && getVariable().equivalent(copy.getVariable());
		}
		return false;
	}
}