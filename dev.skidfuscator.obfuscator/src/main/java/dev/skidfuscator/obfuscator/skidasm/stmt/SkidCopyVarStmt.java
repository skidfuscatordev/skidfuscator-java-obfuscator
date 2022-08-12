package dev.skidfuscator.obfuscator.skidasm.stmt;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;

public class SkidCopyVarStmt extends CopyVarStmt {
    public SkidCopyVarStmt(VarExpr variable, Expr expression) {
        super(variable, expression);

        if (variable == null) {
            throw new IllegalStateException("Var expression cannot be null! " + this.toString());
        }
        else if (variable.getLocal() == null) {
            throw new IllegalStateException("Var local cannot be null! " + this.toString());
        }
    }

    public SkidCopyVarStmt(VarExpr variable, Expr expression, boolean synthetic) {
        super(variable, expression, synthetic);

        if (variable == null) {
            throw new IllegalStateException("Var expression cannot be null! " + this.toString());
        }
        else if (variable.getLocal() == null) {
            throw new IllegalStateException("Var local cannot be null! " + this.toString());
        }
    }
}
