package org.mapleir.deob.dataflow.graph;

import org.mapleir.ir.code.CodeUnit;

/**
 * Represents data flow being passed into a method as an argument expression in an invocation.
 */
public class ArgumentDataflowEdge extends DataflowEdge {
    public final int argNum;

    public ArgumentDataflowEdge(DataflowVertex src, DataflowVertex dst, CodeUnit via, int argNum) {
        super(src, dst, via);
        this.argNum = argNum;
    }

    @Override
    public DataflowType getType() {
        return DataflowType.ARGUMENT;
    }
}
