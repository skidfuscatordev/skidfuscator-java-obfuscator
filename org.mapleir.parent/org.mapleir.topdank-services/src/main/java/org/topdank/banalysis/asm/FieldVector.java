package org.topdank.banalysis.asm;

import java.util.List;

import org.mapleir.asm.FieldNode;
import org.topdank.banalysis.filter.Filter;

public class FieldVector extends InfoVector<FieldNode> {
	
	public FieldVector(List<FieldNode> fields) {
		super(fields);
	}
	
	public FieldVector(List<FieldNode> fields, boolean definiteCount, Filter<FieldNode> filter) {
		super(fields, definiteCount, filter);
	}
}
