package org.mapleir.stdlib.collections.graph.directed;

import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.util.FakeFastEdge;
import org.mapleir.stdlib.collections.graph.util.FakeFastVertex;

public class FakeFastDirectedGraph extends FastDirectedGraph<FakeFastVertex, FakeFastEdge> {
	
	@Override
	public FakeFastEdge clone(FakeFastEdge e, FakeFastVertex src, FakeFastVertex dst) {
		return new FakeFastEdge(src, dst, true);
	}
}
