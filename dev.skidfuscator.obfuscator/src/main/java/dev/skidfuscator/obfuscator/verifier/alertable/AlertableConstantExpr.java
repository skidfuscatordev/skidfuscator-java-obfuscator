package dev.skidfuscator.obfuscator.verifier.alertable;

import org.mapleir.ir.code.expr.ConstantExpr;
import org.objectweb.asm.Type;

public class AlertableConstantExpr extends ConstantExpr {
    public AlertableConstantExpr(Object cst) {
        super(cst);
    }

    public AlertableConstantExpr(Object cst, Type type, boolean check) {
        super(cst, type, check);
    }

    public AlertableConstantExpr(Object cst, Type type) {
        super(cst, type);
    }

    @Override
    public void setConstant(Object o) {
        throw new IllegalStateException("Cannot add object");
    }
}
