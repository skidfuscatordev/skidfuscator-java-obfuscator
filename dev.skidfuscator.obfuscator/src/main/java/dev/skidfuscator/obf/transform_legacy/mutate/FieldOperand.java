package dev.skidfuscator.obf.transform_legacy.mutate;

import java.lang.reflect.Modifier;

import org.mapleir.asm.FieldNode;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.objectweb.asm.Type;

/**
 * @author Cg.
 */
public class FieldOperand<T> implements Operand<T> {
    private Type      type;
    private T         constant;
    private FieldNode representField;
    private Expr      instanceExpr;

    public FieldOperand(Type type, T constant, FieldNode representField, Expr instanceExpr) {
        this.type = type;
        this.constant = constant;
        this.representField = representField;
        this.instanceExpr = instanceExpr;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public T getConstant() {
        return constant;
    }

    public FieldNode getField() {
        return representField;
    }

    public String getFieldOwner() {
        return representField.getOwner();
    }

    public String getFieldName() {
        return representField.getName();
    }

    public String getFieldDesc() {
        return representField.getDesc();
    }

    @Override
    public Expr build() {
        boolean isStatic = false;
        if (Modifier.isStatic(representField.node.access)) {
            isStatic = true;
        }
        Expr instanceExpr = null;
        if (!isStatic) {
            instanceExpr = this.instanceExpr;
        }
        return new FieldLoadExpr(instanceExpr, getFieldOwner(), getFieldName(), getFieldDesc(), isStatic);
    }
}
