package dev.skidfuscator.obf.transform_legacy.mutate;

import org.mapleir.ir.code.Expr;
import org.objectweb.asm.Type;

/**
 * @author Cg.
 */
public interface Operand<T> {

    Type getType();

    T getConstant();

    Expr build();
}
