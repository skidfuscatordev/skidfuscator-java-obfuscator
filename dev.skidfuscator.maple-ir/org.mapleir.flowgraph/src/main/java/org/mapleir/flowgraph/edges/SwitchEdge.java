package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class SwitchEdge<N extends FastGraphVertex> extends AbstractFlowEdge<N> {
	
	public final int value;
	
	public SwitchEdge(N src, N dst, int value) {
		super(SWITCH, src, dst);
		this.value = value;
	}
	
	@Override
	public String toGraphString() {
		return "Case: " + value;
	}
	
	@Override
	public String toString() {
		return String.format("Switch[%d] #%s -> #%s", value, src.getDisplayName(), dst.getDisplayName());
	}

	@Override
	public String toInverseString() {
		return String.format("Switch[%d] #%s <- #%s", value, dst.getDisplayName(), src.getDisplayName());
	}

	@Override
	public SwitchEdge<N> clone(N src, N dst) {
		return new SwitchEdge<>(src, dst, value);
	}
}
