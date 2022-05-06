package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphEdgeImpl;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

import java.util.Objects;

public abstract class AbstractFlowEdge<N extends FastGraphVertex> extends FastGraphEdgeImpl<N> implements FlowEdge<N>, FlowEdges {
	protected final int type;

	public AbstractFlowEdge(int type, N src, N dst) {
		super(src, dst);
		this.type = type;
	}

	public int getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		AbstractFlowEdge<?> that = (AbstractFlowEdge<?>) o;
		return getType() == that.getType();
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getType());
	}
}
