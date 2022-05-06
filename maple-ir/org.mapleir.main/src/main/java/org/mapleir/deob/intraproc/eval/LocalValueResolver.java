package org.mapleir.deob.intraproc.eval;

import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.locals.Local;
import org.mapleir.stdlib.collections.taint.TaintableSet;

/**
 * Provides possible values a local could hold in a CFG.
 */
public interface LocalValueResolver {
	/**
	 *
	 * @param cfg Method to provide value relevant for
	 * @param l Local to provide value for
	 * @return Taintable set of possible values the local could represent
	 */
	TaintableSet<Expr> getValues(ControlFlowGraph cfg, Local l);
}
