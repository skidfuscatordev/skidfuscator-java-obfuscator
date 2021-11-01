package dev.skidfuscator.obf.maple;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;

public class FakeConditionalJumpStmt extends ConditionalJumpStmt {
    public FakeConditionalJumpStmt(Expr left, Expr right, BasicBlock trueSuccessor, ComparisonType type) {
        super(left, right, trueSuccessor, type);
    }


}
