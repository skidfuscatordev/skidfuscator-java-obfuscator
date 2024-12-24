package org.mapleir.stdlib.collections.graph.algorithms;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapleir.stdlib.collections.graph.util.GraphConverter;
import org.mapleir.stdlib.collections.graph.util.OrderedNode;
import org.mapleir.stdlib.collections.graph.util.OrderedNode.ODirectedGraph;
import org.mapleir.stdlib.collections.graph.util.OrderedNode.OGraph;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;

import static org.junit.jupiter.api.Assertions.*;

public class DfsTest {

	ODirectedGraph g;

	@BeforeEach
	public void setUp() {
		try {
			g = (ODirectedGraph) GraphConverter.fromFile("/dfs.gv");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSimpleDfsPre() {
		DepthFirstSearch<OrderedNode> dfs = new SimpleDfs<>(g, getNode(g, 1), SimpleDfs.PRE);
		List<OrderedNode> res = dfs.getPreOrder();
		assertPreOrdered(res);
	}

	@Test
	public void testExtendedDfsPre() {
		DepthFirstSearch<OrderedNode> dfs = new ExtendedDfs<>(g, ExtendedDfs.PRE).run(getNode(g, 1));
		List<OrderedNode> res = dfs.getPreOrder();
		assertPreOrdered(res);
	}

	@Test
	public void testSimpleDfsTopo() {
		DepthFirstSearch<OrderedNode> dfs = new SimpleDfs<>(g, getNode(g, 1), SimpleDfs.TOPO);
		List<OrderedNode> res = dfs.getTopoOrder();
		assertTopoOrdered(res);
	}

	@Test
	public void testExtendedDfsTopo() {
		DepthFirstSearch<OrderedNode> dfs = new ExtendedDfs<>(g, ExtendedDfs.TOPO).run(getNode(g, 1));
		List<OrderedNode> res = dfs.getTopoOrder();
		assertTopoOrdered(res);
	}

	private void assertPreOrdered(List<OrderedNode> nodes) {
		Set<OrderedNode> visited = new HashSet<>();
		assertEquals(new HashSet<>(nodes), g.vertices(), "missing nodes");
		for (int i = 1; i < nodes.size(); i++) {
			OrderedNode node = nodes.get(i);
			visited.add(node);
			OrderedNode prev = nodes.get(i - 1);
			if (!Iterators.all(g.getSuccessors(prev).iterator(), Predicates.in(visited)))
				assertTrue(Iterators.contains(g.getPredecessors(node).iterator(), prev));
		}
	}

	private void assertTopoOrdered(List<OrderedNode> nodes) {
		Set<OrderedNode> visited = new HashSet<>();
		assertEquals(new HashSet<>(nodes), g.vertices());
		for (OrderedNode node : nodes) {
			visited.add(node);
			assertTrue(Iterators.all(g.getPredecessors(node).iterator(), Predicates.in(visited)), "unvisited pred");
		}
	}
	
	public static OrderedNode getNode(OGraph graph, int time) {
		for(OrderedNode n : graph.vertices()) {
			if(n.time == time) {
				return n;
			}
		}
		throw new IllegalStateException(String.format("graph does not contain node with id %d", time));
	}
}
