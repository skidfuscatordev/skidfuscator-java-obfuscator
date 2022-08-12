package org.mapleir.deob.dataflow;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.stdlib.util.JavaDescUse;
import org.mapleir.stdlib.util.JavaDescSpecifier;

import java.util.stream.Stream;

public interface DataFlowAnalysis {
    /**
     * Callback that must be called whenever a CodeUnit is removed from any class the app (IR cache).
     * @param cu removed CodeUnit
     */
    void onRemoved(CodeUnit cu);

    /**
     * Callback that must be called whenever a CodeUnit is added to any class the app (IR cache).
     * @param cu removed codeunit
     */
    void onAdded(CodeUnit cu);

    /**
     * @param jds a JavaDescSpecifier specifying the data flow sources to find usages of
     * @return a stream of all DataflowUses in the app that reference JavaDescs matched
     */
    Stream<JavaDescUse> findAllRefs(JavaDescSpecifier jds);

    /**
     * @return a stream of all constant expressions used in the app
     */
    Stream<ConstantExpr> enumerateConstants();
}
