package org.mapleir.deob.dataflow.graph;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.stdlib.collections.graph.FastGraphEdgeImpl;

import java.util.Objects;

public abstract class DataflowEdge extends FastGraphEdgeImpl<DataflowVertex> {
    public final CodeUnit via; // flow element

    public DataflowEdge(DataflowVertex src, DataflowVertex dst, CodeUnit via) {
        super(src, dst);
        this.via = via;
    }

    @Override
    public String toString() {
        return "(" + getType() + ") " + src + " -> " + dst + " via " + via ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        DataflowEdge that = (DataflowEdge) o;
        if (that.getType() != getType())
            return false;
        return Objects.equals(via, that.via);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), via, getType());
    }

    public abstract DataflowType getType();

    public enum DataflowType {
        FIELD,
        ARGUMENT,
        RETURN,
        CALL
    }
}
