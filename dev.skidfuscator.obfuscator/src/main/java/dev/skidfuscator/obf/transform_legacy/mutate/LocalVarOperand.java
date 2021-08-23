package dev.skidfuscator.obf.transform_legacy.mutate;

import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

/**
 * @author Cg.
 */
public class LocalVarOperand<T> implements Operand<T> {
    private Local local;
    private Type  type;
    private T     result;

    public LocalVarOperand(Local local, Type type, T result) {
        this.local = local;
        this.type = type;
        this.result = result;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public T getConstant() {
        return result;
    }

    public Local getLocal() {
        return local;
    }

    @Override
    public Expr build() {
        return new VarExpr(local, type);
    }
}
