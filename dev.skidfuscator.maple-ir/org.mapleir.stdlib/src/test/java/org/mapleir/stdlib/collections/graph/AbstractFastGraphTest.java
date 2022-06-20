package org.mapleir.stdlib.collections.graph;

import java.util.HashMap;
import java.util.Map;

import org.mapleir.stdlib.collections.graph.util.FakeFastEdge;
import org.mapleir.stdlib.collections.graph.util.FakeFastVertex;

import junit.framework.TestCase;

public abstract class AbstractFastGraphTest extends TestCase {

	private final boolean directed;
	protected final Map<Integer, FakeFastVertex> nodes = new HashMap<>();
	
	public AbstractFastGraphTest(boolean directed) {
		this.directed = directed;
	}
	
	public FakeFastVertex node(int id) {
		return nodes.computeIfAbsent(id, id2 -> new FakeFastVertex(id2));
	}
	
	public FakeFastEdge edge(int src, int dst) {
		return new FakeFastEdge(node(src), node(dst), directed);
	}
	
	public int size() {
		return nodes.size();
	}
}
