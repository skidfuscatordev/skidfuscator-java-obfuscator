package org.mapleir.stdlib.collections.graph;

public interface FastGraphEdge<N extends FastGraphVertex> {
	N src();

	N dst();
}
