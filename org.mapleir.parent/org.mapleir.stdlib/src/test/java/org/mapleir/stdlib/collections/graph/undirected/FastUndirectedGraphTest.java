package org.mapleir.stdlib.collections.graph.undirected;

import static org.mapleir.stdlib.collections.graph.util.CollectionUtil.*;

import org.mapleir.stdlib.collections.graph.AbstractFastGraphTest;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.util.FakeFastEdge;

public class FastUndirectedGraphTest extends AbstractFastGraphTest {

	public FastUndirectedGraphTest() {
		super(false);
	}

	public void testAddVertex() {
		FakeFastUndirectedGraph g = graph();
		assertEquals(0, g.size());
		g.addVertex(node(1));
		assertEquals(1, g.size());
		// add same node
		g.addVertex(node(1));
		assertEquals(1, g.size());
	}
	
	public void testAddVertexByAddEdge() {
		FakeFastUndirectedGraph g = graph();
		FakeFastEdge e = edge(1, 2);
		g.addEdge(e);
		
		assertEquals(2, g.size());
	}
	
	public void testAddEdge() {
		FakeFastUndirectedGraph g = graph();
		FakeFastEdge e = edge(1, 2);
		g.addEdge(e);
		
		assertEquals(1, g.getEdges(node(1)).size());
		assertEquals(1, g.getEdges(node(2)).size());
		
		assertSisterEdges(g.getEdges(node(1)).iterator().next(), g.getEdges(node(2)).iterator().next());
	}

	public void testRemoveEdge() {
		FakeFastUndirectedGraph g = graph();
		FakeFastEdge e = edge(1, 2);
		g.addEdge(e);
		FakeFastEdge sisterE = g.pubGetSisterEdge(e);
		g.removeEdge(e);
		
		// should still have the nodes
		assertEquals(2, g.size());
		assertFalse(g.containsEdge(e));
		assertFalse(g.containsEdge(sisterE));
	}
	
	public void testRemoveVertex() {
		FakeFastUndirectedGraph g = graph();
		FakeFastEdge e1 = edge(1, 2), e2 = edge(1, 3),
				e3 = edge(2, 4), e4 = edge(3, 4);
		g.addEdge(e1);
		g.addEdge(e2);
		g.addEdge(e3);
		g.addEdge(e4);
		
		assertEquals(4, g.size());
		assertContainsBidiEdge(g, e1);
		assertContainsBidiEdge(g, e2);
		assertContainsBidiEdge(g, e3);
		assertContainsBidiEdge(g, e4);

		g.removeVertex(node(2));
		assertEquals(3, g.size());
		assertFalse(g.containsVertex(node(2)));
		assertNotContainsBidiEdge(g, e1);
		assertContainsBidiEdge(g, e2);
		assertNotContainsBidiEdge(g, e3);
		assertContainsBidiEdge(g, e4);
	}
	
	public void testReplace() {
		FakeFastUndirectedGraph g = graph();
		FakeFastEdge e1 = edge(1, 2), e2 = edge(1, 3), e3 = edge(2, 4), e4 = edge(3, 4);
		g.addEdge(e1);
		g.addEdge(e2);
		g.addEdge(e3);
		g.addEdge(e4);
		
		assertEquals(4, g.size());
		assertContainsBidiEdge(g, e1);
		assertContainsBidiEdge(g, e2);
		assertContainsBidiEdge(g, e3);
		assertContainsBidiEdge(g, e4);
		
		g.replace(node(2), node(5));
		
		assertContainsEdges(getEdges(g), asList(edge(1, 5), edge(5, 1), edge(1, 3),
				edge(3, 1), edge(5, 4), edge(4, 5), edge(3, 4), edge(4, 3)));
	}
	
	private void assertContainsBidiEdge(FakeFastUndirectedGraph g, FakeFastEdge e) {
		assertTrue(g.containsEdge(e));
		FakeFastEdge sisterE = g.pubGetSisterEdge(e);
		assertTrue(g.containsEdge(sisterE));
	}
	
	private void assertNotContainsBidiEdge(FakeFastUndirectedGraph g, FakeFastEdge e) {
		assertFalse(g.containsEdge(e));
		FakeFastEdge sisterE = g.pubGetSisterEdge(e);
		assertNull(sisterE);
	}
	
	private <E extends FastGraphEdge<?>> void assertSisterEdges(E e1, E e2) {
		String msg = getSisterErrorMsg(e1, e2);
		assertEquals(msg, e1.src(), e2.dst());
		assertEquals(msg, e1.dst(), e2.src());
	}
	
	private String getSisterErrorMsg(Object o1, Object o2) {
		return o1 + " and " + o2 + " are not sister edges";
	}
	
	private FakeFastUndirectedGraph graph() {
		return new FakeFastUndirectedGraph();
	}
}
