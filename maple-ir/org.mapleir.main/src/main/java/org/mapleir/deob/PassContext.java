package org.mapleir.deob;

import java.util.List;

import org.mapleir.context.AnalysisContext;

public class PassContext {

	private final AnalysisContext cxt;
	private final IPass prev;
	private final List<IPass> completed;
	
	public PassContext(AnalysisContext cxt, IPass prev, List<IPass> completed) {
		this.cxt = cxt;
		this.prev = prev;
		this.completed = completed;
	}

	public AnalysisContext getAnalysis() {
		return cxt;
	}

	public IPass getPrev() {
		return prev;
	}

	public List<IPass> getCompleted() {
		return completed;
	}
}
