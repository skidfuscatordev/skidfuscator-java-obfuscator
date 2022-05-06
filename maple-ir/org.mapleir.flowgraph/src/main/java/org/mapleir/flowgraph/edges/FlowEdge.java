package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public interface FlowEdge<N extends FastGraphVertex> extends FastGraphEdge<N>, FlowEdges {
	
	int getType();
	
	String toGraphString();
	
	@Override
	String toString();
	
	String toInverseString();
	
	FlowEdge<N> clone(N src, N dst);
}
