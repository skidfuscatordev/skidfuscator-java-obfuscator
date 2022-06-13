package org.mapleir.ir.code;

import org.objectweb.asm.Type;
import org.topdank.banalysis.asm.desc.Arrays;

public class ExpressionPool {
    private Type[] renderedTypes;

    public ExpressionPool(Type[] renderedTypes) {
        this.renderedTypes = renderedTypes;
    }

    public void set(final int index, final Type type) {
        assert index < renderedTypes.length : "Provided index is larger than allocated array! " +
                "(" + index + " / " + renderedTypes.length + ")";

        final boolean weirdtype = type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE;

        assert !weirdtype || index + 1 < renderedTypes.length : "Provided expanded index is larger than allocated pool size!";

        renderedTypes[index] = type;

        if (weirdtype) {
            renderedTypes[index] = Type.VOID_TYPE;
        }
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
