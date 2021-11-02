package dev.skidfuscator.obf.maple;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;

public class FakeArithmeticExpr extends ArithmeticExpr {
    public FakeArithmeticExpr(Expr right, Expr left, Operator operator) {
        super(left, right, operator);
    }
}
