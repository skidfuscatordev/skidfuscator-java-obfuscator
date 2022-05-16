package dev.skidfuscator.obfuscator.skidasm.stmt;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.SwitchStmt;

import java.util.LinkedHashMap;

public class SkidSwitchStmt extends SwitchStmt {
    public SkidSwitchStmt(Expr expr, LinkedHashMap<Integer, BasicBlock> targets, BasicBlock defaultTarget) {
        super(expr, targets, defaultTarget);
    }
}
