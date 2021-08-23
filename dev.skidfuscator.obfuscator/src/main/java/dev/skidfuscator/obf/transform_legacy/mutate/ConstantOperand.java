package dev.skidfuscator.obf.transform_legacy.mutate;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.objectweb.asm.Type;

/**
 * @author Cg.
 */
public class ConstantOperand<T> implements Operand<T> {
    private Type type;
    private T    constant;

    public ConstantOperand(Type type, T constant) {
        this.type = type;
        this.constant = constant;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public T getConstant() {
        return constant;
    }

    @Override
    public Expr build() {
        return new ConstantExpr(constant, type, true);
    }
}
