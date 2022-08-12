package org.mapleir.stdlib.collections.graph.util;

import org.mapleir.stdlib.collections.graph.FastGraphEdgeImpl;

public class FakeFastEdge extends FastGraphEdgeImpl<FakeFastVertex> {
	private final boolean directed;
	
	public FakeFastEdge(FakeFastVertex src, FakeFastVertex dst, boolean directed) {
		super(src, dst);
		this.directed = directed;
	}
	
	@Override
	public String toString() {
		return src.getDisplayName() + (directed ? " -> " : " -- ") + dst.getDisplayName();
	}
}
