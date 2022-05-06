package org.mapleir.stdlib.collections.graph.algorithms;

import java.util.*;

import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

// TODO: Convert to stack-invariant
public class TarjanSCC <N extends FastGraphVertex> {
	
	protected final FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph;
	protected final Map<N, Integer> index;
	protected final Map<N, Integer> low;
	protected final LinkedList<N> stack;
	protected final List<List<N>> comps;
	protected int cur;
	
	public TarjanSCC(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph) {
		this.graph = graph;
		
		index = new HashMap<>();
		low = new HashMap<>();
		stack = new LinkedList<>();
		comps = new ArrayList<>();
	}
	
	public int low(N n) {
		return low.getOrDefault(n, -1);
	}
	
	public int index(N n) {
		return index.getOrDefault(n, -1);
	}
	
	public List<List<N>> getComponents() {
		return comps;
	}
	
	public void search(N n) {
		// System.out.println("x: " + n);
		index.put(n, cur);
		low.put(n, cur);
		cur++;
		
		stack.push(n);
		
		for(FastGraphEdge<N> e : filter(getEdges(n))) {
			N s = dst(e);
			if(low.containsKey(s)) {
				if(index.get(s) < index.get(n) && stack.contains(s)) {
					low.put(n, Math.min(low.get(n), index.get(s)));
				}
			} else {
				search(s);
				low.put(n, Math.min(low.get(n), low.get(s)));
			}
		}
		
		if(Objects.equals(low.get(n), index.get(n))) {
			Set<N> c = new HashSet<>();
			
			N w = null;
			do {
				w = stack.pop();
				c.add(w);
			} while (w != n);
			
			comps.add(0, formComponent(c, n));
		}
	}
	
	protected N dst(FastGraphEdge<N> e) {
		return e.dst();
	}

	protected List<N> formComponent(Set<N> s, N found) {
		ExtendedDfs<N> dfs = new ExtendedDfs<>(graph, ExtendedDfs.TOPO).setMask(s).run(found);
		return dfs.getTopoOrder();
	}
	
	protected Set<? extends FastGraphEdge<N>> getEdges(N n) {
		return graph.getEdges(n);
	}

	protected Iterable<? extends FastGraphEdge<N>> filter(Set<? extends FastGraphEdge<N>> edges) {
		return edges;
	}
	
	/* static final Map<Class<?>, Integer> WEIGHTS = new HashMap<>();
	
	{
		WEIGHTS.put(ImmediateEdge.class, 10);
		WEIGHTS.put(ConditionalJumpEdge.class, 9);
		WEIGHTS.put(UnconditionalJumpEdge.class, 8);
		WEIGHTS.put(DefaultSwitchEdge.class, 7);
		WEIGHTS.put(SwitchEdge.class, 6);
		WEIGHTS.put(TryCatchEdge.class, 5);
	}  */

	/* List<FastGraphEdge<N>> weigh(Set<FastGraphEdge<N>> edges) {
		List<FastGraphEdge<N>> list = new ArrayList<>(edges);
		Collections.sort(list, new Comparator<FastGraphEdge<N>>() {
			@Override
			public int compare(FlowEdge<N> o1, FlowEdge<N> o2) {
				Class<?> c1 = o1.getClass();
				Class<?> c2 = o2.getClass();
				
				if(!WEIGHTS.containsKey(c1)) {
					throw new IllegalStateException(c1.toString());
				} else if(!WEIGHTS.containsKey(c2)) {
					throw new IllegalStateException(c2.toString());
				}
				
				int p1 = WEIGHTS.get(c1);
				int p2 = WEIGHTS.get(c2);
				
				// p2, p1 because higher weights are
				// more favoured.
				return Integer.compare(p2, p1);
			}
		});
		System.out.println("list: " + list);
		return list;
	} */
}
