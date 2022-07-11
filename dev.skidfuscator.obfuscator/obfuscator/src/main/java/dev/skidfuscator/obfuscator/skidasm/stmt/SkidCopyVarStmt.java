package dev.skidfuscator.obfuscator.skidasm.stmt;

import dev.skidfuscator.obfuscator.skidasm.expr.SkidVarExpr;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.dynamic.DynamicLocal;

public class SkidCopyVarStmt extends CopyVarStmt {
    public SkidCopyVarStmt(ControlFlowGraph cfg, SkidVarExpr variable, Expr expression) {
        super(variable, expression);

        if (variable == null) {
            throw new IllegalStateException("Var expression cannot be null! " + this.toString());
        }
        else if (variable.getLocal() == null) {
            throw new IllegalStateException("Var local cannot be null! " + this.toString());
        }

        cfg.getDynamicLocals().addDef((DynamicLocal) variable.getLocal(), this);
    }

    public SkidCopyVarStmt(ControlFlowGraph cfg, VarExpr variable, Expr expression, boolean synthetic) {
        super(variable, expression, synthetic);

        if (variable == null) {
            throw new IllegalStateException("Var expression cannot be null! " + this.toString());
        }
        else if (variable.getLocal() == null) {
            throw new IllegalStateException("Var local cannot be null! " + this.toString());
        }

        cfg.getDynamicLocals().addDef((DynamicLocal) variable.getLocal(), this);
    }
}
