package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class DummyEdge<N extends FastGraphVertex> extends AbstractFlowEdge<N>{

	public DummyEdge(N src, N dst) {
		super(DUMMY, src, dst);
	}

	@Override
	public String toGraphString() {
		return "Dummy";
	}

	@Override
	public String toString() {
		return "Dummy #" + src.getDisplayName() + " -> #" + dst.getDisplayName();
	}

	@Override
	public String toInverseString() {
		return "Dummy #" + src.getDisplayName() + " <- #" + dst.getDisplayName();
	}

	@Override
	public FlowEdge<N> clone(N src, N dst) {
		return new DummyEdge<>(src, dst);
	}
}
