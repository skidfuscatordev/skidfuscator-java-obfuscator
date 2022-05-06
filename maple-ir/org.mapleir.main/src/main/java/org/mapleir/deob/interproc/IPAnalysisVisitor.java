package org.mapleir.deob.interproc;

import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.asm.MethodNode;

/**
 * Callbacks for pre/post order interprocedural callgraph searches.
 */
public interface IPAnalysisVisitor {
	/**
	 * Anterior preorder processing.
	 * At this stage, the method has not yet been traced and local-parameter indices are not yet available.
	 */
	default void preVisitMethod(IPAnalysis analysis, MethodNode m) {}
	
	/**
	 * Posterior preorder processing.
	 * At this stage, the method has not yet been traced but local-parameter indices have been computed.
	 */
	default void postVisitMethod(IPAnalysis analysis, MethodNode m) {}
	
	/**
	 * Anterior postorder processing.
	 * At this stage, callgraph information has not yet been updated regarding the invocation.
	 */
	default void preProcessedInvocation(IPAnalysis analysis, MethodNode caller, MethodNode callee, Invocation e) {}
	
	/**
	 * Posterior postorder processing.
	 * At this stage, callgraph information has been updated to include the invocation.
	 */
	default void postProcessedInvocation(IPAnalysis analysis, MethodNode caller, MethodNode callee, Invocation e) {}
}
