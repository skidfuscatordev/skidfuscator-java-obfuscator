package org.mapleir.ir.code.expr.invoke;

import org.mapleir.ir.code.Expr;

public interface Invokable {
    Expr[] getArgumentExprs();

    Expr[] getParameterExprs();

    String getOwner();

    String getName();

    String getDesc();
    
    void setArgumentExprs(final Expr[] args);

    void setOwner(final String owner);

    void setName(final String name);

    void setDesc(final String desc);
}
