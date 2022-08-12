package org.mapleir.deob.dataflow;

import org.mapleir.context.IRCache;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.stdlib.util.IUsesJavaDesc;
import org.mapleir.stdlib.util.JavaDescUse;
import org.mapleir.stdlib.util.JavaDescSpecifier;

import java.util.stream.Stream;

/**
 * A very naive DataFlowAnalysis implementation that doesn't do any result caching.
 * This means that it recomputes the results every time.
 */
public class LiveDataFlowAnalysisImpl implements DataFlowAnalysis {
    private final IRCache irCache;

    public LiveDataFlowAnalysisImpl(IRCache irCache) {
        this.irCache = irCache;
    }

    @Override
    public void onRemoved(CodeUnit cu) {

    }

    @Override
    public void onAdded(CodeUnit cu) {

    }

    @Override
   	public Stream<JavaDescUse> findAllRefs(JavaDescSpecifier jds) {
   		return irCache.allExprStream()
                .filter(cu -> cu instanceof IUsesJavaDesc)
                .map(cu -> (IUsesJavaDesc) cu)
                .filter(cu -> jds.matches((cu.getJavaDesc())))
                .map(IUsesJavaDesc::getDataUse);
   	}

   	@Override
    public Stream<ConstantExpr> enumerateConstants() {
        return irCache.allExprStream().filter(cu -> cu instanceof ConstantExpr).map(cu -> (ConstantExpr) cu);
    }
}
