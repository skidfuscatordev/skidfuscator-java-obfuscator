package org.mapleir.stdlib.collections.graph.algorithms;

import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

import java.util.*;

/**
 * Deprecated. Use ExtendedDfs instead.
 */
@Deprecated
public class SimpleDfs<N extends FastGraphVertex> implements DepthFirstSearch<N> {
	public static final int REVERSE = ExtendedDfs.REVERSE, PRE = ExtendedDfs.PRE, POST = ExtendedDfs.POST, TOPO = ExtendedDfs.TOPO;

	private ExtendedDfs<N> impl;
	
	public SimpleDfs(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry, int flags) {
		impl = new ExtendedDfs<>(graph, flags).run(entry);
	}

	public static <N extends FastGraphVertex> List<N> preorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry) {
		return preorder(graph, entry, false);
	}
	
	public static <N extends FastGraphVertex> List<N> preorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry, boolean reverse) {
		return new SimpleDfs<>(graph, entry, PRE | (reverse? REVERSE : 0)).getPreOrder();
	}
	
	public static <N extends FastGraphVertex> List<N> postorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry) {
		return postorder(graph, entry, false);
	}
	
	public static <N extends FastGraphVertex> List<N> postorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry, boolean reverse) {
		return new SimpleDfs<>(graph, entry, POST | (reverse? REVERSE : 0)).getPostOrder();
	}

	public static <N extends FastGraphVertex> List<N> topoorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry) {
		return topoorder(graph, entry, false);
	}

	public static <N extends FastGraphVertex> List<N> topoorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry, boolean reverse) {
		return new SimpleDfs<>(graph, entry, TOPO | POST | (reverse? REVERSE : 0)).getTopoOrder();
	}

	@Override
	public List<N> getPreOrder() {
		return impl.getPreOrder();
	}

	@Override
	public List<N> getPostOrder() {
		return impl.getPostOrder();
	}

	@Override
	public List<N> getTopoOrder() {
		return impl.getTopoOrder();
	}
}
