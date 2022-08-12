package org.mapleir.stdlib.collections.graph.directed;

import static org.mapleir.stdlib.collections.graph.util.CollectionUtil.*;

import org.mapleir.stdlib.collections.graph.AbstractFastGraphTest;
import org.mapleir.stdlib.collections.graph.util.FakeFastEdge;

public class FastDirectedGraphTest extends AbstractFastGraphTest {
	
	public FastDirectedGraphTest() {
		super(true);
	}

	public void testAddVertex() {
		FakeFastDirectedGraph g = graph();
		assertEquals(0, g.size());
//		assertEquals(0, g.reverseSize());
		g.addVertex(node(1));
		assertEquals(1, g.size());
//		assertEquals(1, g.reverseSize());
		// add same node
		g.addVertex(node(1));
		assertEquals(1, g.size());
//		assertEquals(1, g.reverseSize());
	}
	
	public void testAddVertexByAddEdge() {
		FakeFastDirectedGraph g = graph();
		FakeFastEdge e = edge(1, 2);
		g.addEdge(e);
		
		assertEquals(2, g.size());
//		assertEquals(2, g.reverseSize());
	}
	
	public void testAddEdge() {
		FakeFastDirectedGraph g = graph();
		FakeFastEdge e = edge(1, 2);
		g.addEdge(e);
		
		assertEquals(1, g.getEdges(node(1)).size());
		assertEquals(0, g.getEdges(node(2)).size());
		assertEquals(0, g.getReverseEdges(node(1)).size());
		assertEquals(1, g.getReverseEdges(node(2)).size());
		assertTrue(g.containsEdge(e));
		assertTrue(g.containsReverseEdge(e));
	}
	
	public void testRemoveEdge() {
		FakeFastDirectedGraph g = graph();
		FakeFastEdge e = edge(1, 2);
		g.addEdge(e);
		g.removeEdge(e);
		
		// should still have the nodes (?)
		assertEquals(2, g.size());
		assertFalse(g.containsEdge(e));
		assertFalse(g.containsReverseEdge(e));
	}
	
	public void testRemoveVertex() {
		FakeFastDirectedGraph g = graph();
		FakeFastEdge e1 = edge(1, 2), e2 = edge(1, 3),
				e3 = edge(2, 4), e4 = edge(3, 4);
		g.addEdge(e1);
		g.addEdge(e2);
		g.addEdge(e3);
		g.addEdge(e4);
		
		assertEquals(4, g.size());
		assertTrue(g.containsEdge(e1));
		assertTrue(g.containsEdge(e2));
		assertTrue(g.containsEdge(e3));
		assertTrue(g.containsEdge(e4));

		g.removeVertex(node(2));
		assertEquals(3, g.size());
		assertFalse(g.containsVertex(node(2)));
		assertFalse(g.containsEdge(e1));
		assertTrue(g.containsEdge(e2));
		assertFalse(g.containsEdge(e3));
		assertTrue(g.containsEdge(e4));
	}
	
	public void testReplace() {
		FakeFastDirectedGraph g = graph();
		FakeFastEdge e1 = edge(1, 2), e2 = edge(1, 3), e3 = edge(2, 4), e4 = edge(3, 4);
		g.addEdge(e1);
		g.addEdge(e2);
		g.addEdge(e3);
		g.addEdge(e4);
		
		assertEquals(4, g.size());
		assertTrue(g.containsEdge(e1));
		assertTrue(g.containsEdge(e2));
		assertTrue(g.containsEdge(e3));
		assertTrue(g.containsEdge(e4));
		
		g.replace(node(2), node(5));
		
		assertContainsEdges(getEdges(g), asList(edge(1, 5), edge(1, 3), edge(5, 4), edge(3, 4)));
	}
	
	/* internal test */
	public void testClone() {
		FakeFastDirectedGraph g = graph();
		FakeFastEdge e1 = edge(1, 2);
		
		FakeFastEdge e12 = g.clone(e1, node(4), node(5));
		assertEquals(node(4), e12.src());
		assertEquals(node(5), e12.dst());
	}
	
	private FakeFastDirectedGraph graph() {
		return new FakeFastDirectedGraph();
	}
}
