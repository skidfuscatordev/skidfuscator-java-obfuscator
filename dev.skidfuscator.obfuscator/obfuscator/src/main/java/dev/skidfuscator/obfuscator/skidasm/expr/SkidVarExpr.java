package dev.skidfuscator.obfuscator.skidasm.expr;

import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.dynamic.DynamicLocal;
import org.objectweb.asm.Type;

public class SkidVarExpr extends VarExpr {
    public SkidVarExpr(ControlFlowGraph cfg, DynamicLocal local, Type type) {
        super(local, type);

        cfg.getDynamicLocals().addUse(local, this);
    }
}
