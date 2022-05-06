package org.topdank.banalysis.asm;

import java.util.List;

import org.mapleir.asm.MethodNode;
import org.topdank.banalysis.filter.Filter;

public class MethodVector extends InfoVector<MethodNode> {
	
	public MethodVector(List<MethodNode> methods) {
		super(methods);
	}
	
	public MethodVector(List<MethodNode> methods, boolean definiteCount, Filter<MethodNode> filter) {
		super(methods, definiteCount, filter);
	}
}
