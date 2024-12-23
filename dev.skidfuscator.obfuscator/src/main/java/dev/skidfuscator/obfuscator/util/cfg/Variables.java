package dev.skidfuscator.obfuscator.util.cfg;

import lombok.experimental.UtilityClass;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;

@UtilityClass
public class Variables {

    public Expr getDefinition(final ControlFlowGraph cfg, final VarExpr var) {
        Expr arg0 = var;
        while (true) {
            if (arg0 instanceof VarExpr varExpr) {
                Expr arg0_1 = cfg.getAllParents(arg0.getBlock())
                        .stream()
                        .filter(e -> e.stream()
                                .filter(s -> s instanceof CopyVarStmt)
                                .map(CopyVarStmt.class::cast)
                                .anyMatch(s -> s.getVariable().getLocal().equals(varExpr.getLocal()))
                        )
                        .findFirst()
                        .map(e -> e.stream()
                                .filter(s -> s instanceof CopyVarStmt)
                                .map(CopyVarStmt.class::cast)
                                .filter(s -> s.getVariable().getLocal().equals(varExpr.getLocal()))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new)
                        )
                        .map(AbstractCopyStmt::getExpression)
                        .orElse(null);

                if (arg0_1 != null) {
                    arg0 = arg0_1;
                    continue;
                }
            }

            break;
        }

        return arg0;
    }
}
