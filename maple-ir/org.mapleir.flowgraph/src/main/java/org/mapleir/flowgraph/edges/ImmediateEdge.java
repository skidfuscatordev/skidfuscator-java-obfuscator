package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class ImmediateEdge<N extends FastGraphVertex> extends AbstractFlowEdge<N> {
	
	public ImmediateEdge(N src, N dst) {
		super(IMMEDIATE, src, dst);
	}

	@Override
	public String toGraphString() {
		return "Immediate";
	}

	@Override
	public String toString() {
		return String.format("Immediate #%s -> #%s", src.getDisplayName(), dst.getDisplayName());
	}

	@Override
	public String toInverseString() {
		return String.format("Immediate #%s <- #%s", dst.getDisplayName(), src.getDisplayName());
	}
	
	@Override
	public ImmediateEdge<N> clone(N src, N dst) {
		return new ImmediateEdge<>(src, dst);
	}
}
