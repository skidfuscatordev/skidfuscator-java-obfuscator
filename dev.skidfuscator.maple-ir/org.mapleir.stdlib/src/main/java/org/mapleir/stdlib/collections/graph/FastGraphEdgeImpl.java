package org.mapleir.stdlib.collections.graph;

import java.util.Objects;

public class FastGraphEdgeImpl<N extends FastGraphVertex> implements FastGraphEdge<N> {
	protected final N src, dst;

	public FastGraphEdgeImpl(N src, N dst) {
		this.src = src;
		this.dst = dst;
	}

	@Override
	public N src() {
		return src;
	}

	@Override
	public N dst() {
		return dst;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof FastGraphEdge))
			return false;
		FastGraphEdge v = (FastGraphEdge) o;

		// assert consistency
		assert ((v.src().getNumericId() == src.getNumericId()) == (v.src() == src));
		assert ((v.dst().getNumericId() == dst.getNumericId()) == (v.dst() == dst));

		return v.src() == src && v.dst() == dst;
	}

	@Override
	public int hashCode() {
		// remember, we can't assume the numeric id remains constant!
		return Objects.hash(src, dst);
	}
}
