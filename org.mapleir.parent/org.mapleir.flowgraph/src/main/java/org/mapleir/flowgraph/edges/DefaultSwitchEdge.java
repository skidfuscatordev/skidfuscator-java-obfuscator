package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class DefaultSwitchEdge<N extends FastGraphVertex> extends AbstractFlowEdge<N> {
	
	public DefaultSwitchEdge(N src, N dst) {
		super(DEFAULT_SWITCH, src, dst);
	}
	
	@Override
	public String toGraphString() {
		return "Default case";
	}
	
	@Override
	public String toString() {
		return String.format("DefaultSwitch #%s -> #%s", src.getDisplayName(), dst.getDisplayName());
	}
	
	@Override
	public String toInverseString() {
		return String.format("DefaultSwitch #%s <- #%s", dst.getDisplayName(), src.getDisplayName());
	}

	@Override
	public DefaultSwitchEdge<N> clone(N src, N dst) {
		return new DefaultSwitchEdge<>(src, dst);
	}
}
