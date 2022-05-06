package org.mapleir.stdlib.collections.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.propertyframework.api.IPropertyDictionary;

public abstract class FastUndirectedGraph<N extends FastGraphVertex, E extends FastGraphEdge<N>> implements FastGraph<N, E> {

	private final Map<N, Set<E>> edgeSet;
	private final Map<E, E> sisterEdges;
	
	public FastUndirectedGraph() {
		edgeSet = createMap();
		sisterEdges = new HashMap<>();
	}
	
	public FastUndirectedGraph(FastUndirectedGraph<N, E> g) {
		edgeSet = createMap(g.edgeSet);
		sisterEdges = new HashMap<>(g.sisterEdges);
	}

	@Override
	public Set<N> vertices() {
		return new HashSet<>(edgeSet.keySet());
	}

	@Override
	public boolean addVertex(N n) {
		/* putIfAbsent requires Set instantiation even if this node
		 * is already mapped */
		if(edgeSet.containsKey(n)) {
			return false;
		} else {
			edgeSet.put(n, createSet());
			return true;
		}
	}

	protected E getSisterEdge(E e) {
		if(sisterEdges.containsKey(e)) {
			return sisterEdges.get(e);
		} else {
			throw new IllegalArgumentException(
					String.format("Edge is not mapped: %s", e));
		}
	}
	
	@Override
	public void removeVertex(N n) {
		Set<E> edges = edgeSet.remove(n);
		
		if(edges != null && !edges.isEmpty()) {
			for(E edge : edges) {
				E sisterEdge = getSisterEdge(edge);
				
				E e1 = sisterEdges.remove(edge);
				E e2 = sisterEdges.remove(sisterEdge);
				
				assert e1 == sisterEdge;
				assert e2 == edge;
				
				edgeSet.get(edge.dst()).remove(sisterEdge);
			}
		}
	}

	@Override
	public boolean containsVertex(N n) {
		return edgeSet.containsKey(n);
	}

	@Override
	public void addEdge(E e) {
		if(containsEdge(e)) {
			return;
		}
		
		N src = e.src();
		N dst = e.dst();
		
		if(!containsVertex(src)) {
			addVertex(src);
		}
		if(!containsVertex(dst)) {
			addVertex(dst);
		}

		E sisterE = clone(e, dst, src);
		
		sisterEdges.put(e, sisterE);
		sisterEdges.put(sisterE, e);
		
		edgeSet.get(src).add(e);
		edgeSet.get(dst).add(sisterE);
	}

	@Override
	public void removeEdge(E e) {
		if(!containsEdge(e)) {
			return;
		}
		
		N src = e.src();
		N dst = e.dst();
		
		E sisterE = getSisterEdge(e);
		edgeSet.get(src).remove(e);
		edgeSet.get(dst).remove(sisterE);
	}

	@Override
	public boolean containsEdge(E e) {
		N src = e.src();
		return edgeSet.containsKey(src) && edgeSet.get(src).contains(e);
	}

	@Override
	public Set<E> getEdges(N n) {
		return new HashSet<>(edgeSet.get(n));
	}

	@Override
	public int size() {
		return edgeSet.size();
	}

	@Override
	public void replace(N oldNode, N newNode) {
		Set<E> edges = new HashSet<>(edgeSet.get(oldNode));
		
		for(E e : edges) {
			assert e.src() == oldNode;
			E newEdge = clone(e, newNode, e.dst());
			removeEdge(e);
			addEdge(newEdge);
		}
		
		removeVertex(oldNode);
	}

	@Override
	public void clear() {
		edgeSet.clear();
		sisterEdges.clear();
	}

	@Override
	public DotGraph makeDotGraph(IPropertyDictionary properties) {
		/* getEdge(n) returns edges to all nodes that n is connected to, so
		 * each added edge actually has two edges in the dot graph. we need to
		 * remove one edge from each pair from each node. */
		Set<E> addedEdges = new HashSet<>();
		return GraphUtils.makeDotSkeleton(this, null, (ourEdge, dotEdge) -> {
			if(addedEdges.contains(ourEdge)) {
				/* added this edge already */
				return false;
			} else {
				addedEdges.add(ourEdge);
				addedEdges.add(getSisterEdge(ourEdge));
				return true;
			}
		});		
	}
}
