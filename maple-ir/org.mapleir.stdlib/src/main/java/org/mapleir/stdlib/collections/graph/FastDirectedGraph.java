package org.mapleir.stdlib.collections.graph;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.propertyframework.api.IPropertyDictionary;

public abstract class FastDirectedGraph<N extends FastGraphVertex, E extends FastGraphEdge<N>> implements FastGraph<N, E>{

	private final Map<N, Set<E>> map;
	private final Map<N, Set<E>> reverseMap;
	
	public FastDirectedGraph() {
		map = createMap();
		reverseMap = createMap();
	}
	
	public FastDirectedGraph(FastDirectedGraph<N, E> g) {
		map = createMap(g.map);
		reverseMap = createMap(g.reverseMap);
	}

	/**
	 * *NO* guarantee about order vertices will be returned in. Use a search if you need a specific order!
	 */
	@Override
	public Set<N> vertices() {
		return Collections.unmodifiableSet(map.keySet());
	}

	@Override
	public boolean addVertex(N v) {
		boolean ret = false;
		if(!map.containsKey(v)) {
			map.put(v, createSet());
			ret = true;
		}
		
		if(!reverseMap.containsKey(v)) {
			
			if(!ret) {
				throw new IllegalStateException(v.toString());
			}
			
			reverseMap.put(v, createSet());
			ret = true;
		}
		return ret;
	}

	@Override
	public void removeVertex(N v) {
		// A = {(A->B), (A->C)}
		// B = {(B->D)}
		// C = {(C->D)}
		// D = {}
		//  reverse
		// A = {}
		// B = {(A->B)}
		// C = {(A->C)}
		// D = {(B->D), (C->D)}
		
		// if we remove B, the map should
		// now be:
		
		// A = {(A->B), (A->C)}
		//   for(E e : map.remove(B) = {(B->D)}) {
		//      reverseMap.get(e.dst).remove(e);
		//   }
		// C = {(C->D)}
		// D = {}
		//  reverse
		// A = {}
		//   for(E e : reverseMap.remove(B) = {(A->B)}) {
		//      map.get(e.src).remove(e);
		//   }
		// C = {(A->C)}
		// D = {(B->D), (C->D)}
		
		// so now, B has been completely removed
		// A = {(A->C)}
		// 
		// C = {(C->D)}
		// D = {}
		//  reverse
		// A = {}
		// 
		// C = {(A->C)}
		// D = {(C->D)}

		for(E e : map.remove(v)) {
			reverseMap.get(e.dst()).remove(e);
		}
		
		for(E e : reverseMap.remove(v)) {
			map.get(e.src()).remove(e);
		}
	}

	@Override
	public boolean containsVertex(N v) {
		return map.containsKey(v);
	}
	
	public boolean containsReverseVertex(N v) {
		return reverseMap.containsKey(v);
	}

	@Override
	public void addEdge(E e) {
		N src = e.src();
		addVertex(src);
		map.get(src).add(e);
		
		N dst = e.dst();
		addVertex(dst);
		reverseMap.get(dst).add(e);
	}

	@Override
	public void removeEdge(E e) {
		N src = e.src();
		if(map.containsKey(src)) {
			map.get(src).remove(e);
		}
		// we need to remove the edge from dst <- src map
		N dst = e.dst();
		if(reverseMap.containsKey(dst)) {
			reverseMap.get(dst).remove(e);
		}
	}

	@Override
	public boolean containsEdge(E e) {
		N src = e.src();
		return map.containsKey(src) && map.get(src).contains(e);
	}

	public boolean containsReverseEdge(E e) {
		N dst = e.dst();
		return reverseMap.containsKey(dst) && reverseMap.get(dst).contains(e);
	}

	@Override
	public Set<E> getEdges(N b) {
		return Collections.unmodifiableSet(map.get(b));
	}

	public Stream<N> getSuccessors(N v) {
		return getEdges(v).stream().map(E::dst);
	}

	public Set<E> getReverseEdges(N v) {
		return Collections.unmodifiableSet(reverseMap.get(v));
	}

	public Stream<N> getPredecessors(N v) {
		return getReverseEdges(v).stream().map(E::src);
	}

	@Override
	public int size() {
		return map.size();
	}
	
	// TODO: entries
	@Override
	public void replace(N old, N n) {
		// A = {(A->B), (A->C)}
		// B = {(B->D)}
		// C = {(C->D)}
		// D = {}
		//  reverse
		// A = {}
		// B = {(A->B)}
		// C = {(A->C)}
		// D = {(B->D), (C->D)}
		
		// replacing B with E
		
		// A = {(A->E), (A->C)}
		// E = {(E->D)}
		// C = {(C->D)}
		// D = {}
		//  reverse
		// A = {}
		// E = {(A->E)}
		// C = {(A->C)}
		// D = {(E->D), (C->D)}
		
		Set<E> succs = getEdges(old);
		Set<E> preds = getReverseEdges(old);
		
		addVertex(n);
		
		for(E succ : new HashSet<>(succs)) {
			/* 'old' is the 'src' here, change 'n' to the new 'src' */
			E newEdge = clone(succ, n, succ.dst());
			removeEdge(succ);
			addEdge(newEdge);
		}
		
		for(E pred : new HashSet<>(preds)) {
			/* 'old' is the 'dst' here, change 'n' to the new 'dst' */
			E newEdge = clone(pred, pred.src(), n);
			removeEdge(pred);
			addEdge(newEdge);
		}
		
		removeVertex(old);
	}

	@Override
	public void clear() {
		map.clear();
		reverseMap.clear();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("map {\n");
		for(Entry<N, Set<E>> e : map.entrySet()) {
			sb.append("   ").append(e.getKey()).append("  ").append(e.getValue()).append("\n");
		}
		sb.append("}\n");
		
		sb.append("reverse {\n");
		for(Entry<N, Set<E>> e : reverseMap.entrySet()) {
			sb.append("   ").append(e.getKey()).append("  ").append(e.getValue()).append("\n");
		}
		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public DotGraph makeDotGraph(IPropertyDictionary properties) {
		return GraphUtils.makeDotSkeleton(this).setDirected(true);
	}
}
