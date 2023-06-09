package org.mapleir.flowgraph;

import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.TryCatchEdge;
import org.mapleir.stdlib.collections.bitset.BitSetIndexer;
import org.mapleir.stdlib.collections.bitset.GenericBitSet;
import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.algorithms.SimpleDfs;
import org.mapleir.stdlib.collections.map.ValueCreator;

import java.util.*;

public abstract class FlowGraph<N extends FastGraphVertex, E extends FlowEdge<N>> extends FastDirectedGraph<N, E> implements ValueCreator<GenericBitSet<N>> {
	
	protected final List<ExceptionRange<N>> ranges;
	protected final Set<N> entries;
	
	protected final BitSetIndexer<N> indexer;
	protected final Map<Integer, N> indexMap;
	protected final BitSet indexedSet;
	
	public FlowGraph() {
		ranges = new ArrayList<>();
		entries = new HashSet<>();

		indexer = new FastGraphVertexBitSetIndexer();
		indexMap = new HashMap<>();
		indexedSet = new BitSet();
	}
	
	public FlowGraph(FlowGraph<N, E> g) {
		super(g);
		
		ranges = new ArrayList<>(g.ranges);
		entries = new HashSet<>(g.entries);

		indexer = g.indexer;
		indexMap = new HashMap<>(g.indexMap);
		indexedSet = g.indexedSet;
	}
	
	public Set<N> getEntries() {
		return entries;
	}

	/**
	 * Use this if you need a topoorder. There is *NO* guarantee what order vertices() will return the blocks in
	 */
	private List<N> topoorderCache;
	public List<N> verticesInOrder() {
		assert (getEntries().size() == 1);
		if (topoorderCache == null)
			topoorderCache = SimpleDfs.topoorder(this, getEntries().iterator().next());
		return topoorderCache;
	}
	
	public void addRange(ExceptionRange<N> range) {
		if(!ranges.contains(range)) {
			ranges.add(range);
		}
	}

	public void addRange(int index, ExceptionRange<N> range) {
		if(!ranges.contains(range)) {
			ranges.add(index, range);
		}
	}
	
	public void removeRange(ExceptionRange<N> range) {
		ranges.remove(range);
	}
	
	public List<ExceptionRange<N>> getRanges() {
		return new ArrayList<>(ranges);
	}
	
	@Override
	public void clear() {
		super.clear();
		indexMap.clear();
		indexedSet.clear();
		topoorderCache = null;
	}
	
	@Override
	public boolean addVertex(N v) {
		boolean ret = super.addVertex(v);
		
		int index = v.getNumericId();
		assert(!indexMap.containsKey(index) || indexMap.get(index) == v); // ensure no id collisions
		indexMap.put(index, v);
		indexedSet.set(index, true);
		return ret;
	}

	@Override
	public void addEdge(E e) {
		super.addEdge(e);

		N src = e.src();
		int index = src.getNumericId();
		assert(!indexMap.containsKey(index) || indexMap.get(index) == src); // ensure no id collisions
		indexMap.put(index, src);
		indexedSet.set(index, true);
		topoorderCache = null;
	}
	
	@Override
	public void replace(N old, N n) {
		if(entries.contains(old)) {
			entries.add(n);
		}
		topoorderCache = null;
		super.replace(old, n);
	}
	
	@Override
	public void removeVertex(N v) {
		ListIterator<ExceptionRange<N>> it = ranges.listIterator();
		while(it.hasNext()) {
			ExceptionRange<N> r = it.next();
			if (r.containsVertex(v)) {
				r.removeVertex(v);
				if (r.getNodes().isEmpty()) {
					it.remove();
				}
			}
		}
		
		entries.remove(v);
		topoorderCache = null;
		super.removeVertex(v);

		int index = v.getNumericId();
		indexMap.remove(index);
		indexedSet.set(index, false);
	}

	// this is some pretty bad code duplication but it's not too big of a deal.
	public Set<N> dfsNoHandlers(N from, N to) {
		Set<N> visited = new HashSet<>();
		Deque<N> stack = new ArrayDeque<>();
		stack.push(from);
		
		while(!stack.isEmpty()) {
			N s = stack.pop();
			
			Set<E> edges = getEdges(s);
			for(FlowEdge<N> e : edges) {
				if(e instanceof TryCatchEdge)
					continue;
				N next = e.dst();
				if(next != to && !visited.contains(next)) {
					stack.push(next);
					visited.add(next);
				}
			}
		}
		
		visited.add(from);
		
		return visited;
	}

	public GenericBitSet<N> createBitSet() {
		return new GenericBitSet<>(indexer);
	}

	public GenericBitSet<N> createBitSet(Collection<N> other) {
		GenericBitSet<N> set = createBitSet();
		set.addAll(other);
		return set;
	}

	@Override
	public GenericBitSet<N> create() {
		return createBitSet();
	}

	private class FastGraphVertexBitSetIndexer implements BitSetIndexer<N> {
		@Override
		public int getIndex(N basicBlock) {
			return basicBlock.getNumericId();
		}

		@Override
		public N get(int index) {
			// really, we don't want to be using this since it pretty much defeats the point of the whole bitset scheme.
			return indexMap.get(index);
		}

		@Override
		public boolean isIndexed(N basicBlock) {
			return basicBlock != null && indexedSet.get(getIndex(basicBlock));
		}
	}
}
