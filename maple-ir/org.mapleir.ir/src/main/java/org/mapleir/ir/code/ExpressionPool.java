package org.mapleir.ir.code;

import org.objectweb.asm.Type;
import org.topdank.banalysis.asm.desc.Arrays;

public class ExpressionPool {
    private Type[] renderedTypes;

    public ExpressionPool(Type[] renderedTypes) {
        this.renderedTypes = renderedTypes;
    }

    public Type[] getRenderedTypes() {
        return renderedTypes;
    }

    public void setRenderedTypes(Type[] renderedTypes) {
        this.renderedTypes = renderedTypes;
    }

    public ExpressionPool copy() {
        return new ExpressionPool(renderedTypes);
    }
}
