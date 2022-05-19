package org.mapleir.stdlib.collections.graph.undirected;

import org.mapleir.stdlib.collections.graph.FastUndirectedGraph;
import org.mapleir.stdlib.collections.graph.util.FakeFastEdge;
import org.mapleir.stdlib.collections.graph.util.FakeFastVertex;

import java.util.Collection;

public class FakeFastUndirectedGraph extends FastUndirectedGraph<FakeFastVertex, FakeFastEdge> {

	public FakeFastEdge pubGetSisterEdge(FakeFastEdge e) {
		try {
			return super.getSisterEdge(e);
		} catch(Throwable ex) {
			return null;
		}
	}
	
	@Override
	public FakeFastEdge clone(FakeFastEdge e, FakeFastVertex src, FakeFastVertex dst) {
		return new FakeFastEdge(src, dst, false);
	}

	@Override
	public Collection<FakeFastVertex> getCommonAncestor(Collection<FakeFastVertex> nodes) {
		return null;
	}

	@Override
	public String toString() {
		return makeDotGraph().toString();
	}
}
