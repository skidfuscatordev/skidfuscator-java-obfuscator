package org.mapleir.deob.dataflow.graph;

import org.mapleir.ir.code.CodeUnit;

/**
 * Represents a store or load of a field.
 */
public class FieldDataflowEdge extends DataflowEdge {
    public FieldDataflowEdge(DataflowVertex src, DataflowVertex dst, CodeUnit via) {
        super(src, dst, via);
    }

    @Override
    public DataflowType getType() {
        return DataflowType.FIELD;
    }
}
