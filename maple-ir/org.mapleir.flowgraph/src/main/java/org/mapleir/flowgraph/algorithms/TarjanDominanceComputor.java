package org.mapleir.flowgraph.algorithms;

import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphEdgeImpl;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;

import java.util.*;
import java.util.Map.Entry;

/**
 * Simple dominance analyser based on dominance frontiers.
 */
public class TarjanDominanceComputor<N extends FastGraphVertex> {
	private final FastDirectedGraph<N, ?> graph;
	private final List<N> preOrder;
	private final Map<N, Integer> semiIndices;
	private final Map<N, N> parents;
	private final Map<N, N> propagationMap;
	private final Map<N, N> ancestors;
	private final Map<N, N> idoms;
	private final NullPermeableHashMap<N, Set<N>> semiDoms;
	private final NullPermeableHashMap<N, Set<N>> treeChildren;
	private final NullPermeableHashMap<N, Set<N>> frontiers;
	private final NullPermeableHashMap<N, Set<N>> iteratedFrontiers;
	
	public TarjanDominanceComputor(FastDirectedGraph<N, ?> graph, List<N> preOrder) {
		this.graph = graph;
		this.preOrder = preOrder;
		semiIndices = new HashMap<>();
		parents = new HashMap<>();
		propagationMap = new HashMap<>();
		ancestors = new HashMap<>();
		idoms = new HashMap<>();
		semiDoms = new NullPermeableHashMap<>(HashSet::new);
		treeChildren = new NullPermeableHashMap<>(HashSet::new);
		frontiers = new NullPermeableHashMap<>(HashSet::new);
		iteratedFrontiers = new NullPermeableHashMap<>(HashSet::new);
		
		computePreOrder();
		computeDominators();
		touchTree();
		computeFrontiers();
		computeIteratedFrontiers();
	}
	
	public DominatorTree<N> makeTree() {
		DominatorTree<N> dom_tree = new DominatorTree<>();
		for (Entry<N, Set<N>> e : getTree().entrySet()) {
			N b = e.getKey();
			if(b == null) {
				continue;
			}
			dom_tree.addVertex(b);
			for (N c : e.getValue()) {
				dom_tree.addEdge(new FastGraphEdgeImpl<>(b, c));
			}
		}
		return dom_tree;
	}
	
	public Map<N, Set<N>> getTree() {
		return treeChildren;
	}
	
	public Set<N> getTreeChildren(N n) {
		return treeChildren.getNonNull(n);
	}
	
	// FIXME: doesn't seem to hold what is expected
	//private Set<N> semiDoms(N n) {
	//	return semiDoms.getNonNull(n);
	//}
	
	public Set<N> frontier(N n) {
		return frontiers.getNonNull(n);
	}
	
	public Set<N> iteratedFrontier(N n) {
		return iteratedFrontiers.getNonNull(n);
	}
	
	public N idom(N n) {
		return idoms.get(n);
	}

	private void touchTree() {
		for(N n : idoms.keySet()) {
			treeChildren.getNonNull(idoms.get(n)).add(n);
		}
	}
	
	private Iterator<N> topoSort() {
		LinkedList<N> list = new LinkedList<>();
		for(N n : preOrder) {
			int i = list.indexOf(idoms.get(n));
			if(i == -1) {
				list.add(n);
			} else {
				list.add(i + 1, n);
			}
		}
		return list.descendingIterator();
	}
	
	private void computeIteratedFrontiers() {
		for(N n : preOrder) {
			computeIteratedFrontiers(n);
		}
	}
	
	private void computeIteratedFrontiers(N n) {
		Set<N> res = new HashSet<>();
		
		Set<N> workingSet = new HashSet<>();
		workingSet.add(n);
		
		do {
			Set<N> newWorkingSet = new HashSet<>();
			Iterator<N> it = workingSet.iterator();
			while(it.hasNext()) {
				N n1 = it.next();
				
				Iterator<N> dfIt = frontier(n1).iterator();
				while(dfIt.hasNext()) {
					N n2 = dfIt.next();
					if(!res.contains(n2)) {
						newWorkingSet.add(n2);
						res.add(n2);
					}
				}
			}
			workingSet = newWorkingSet;
		} while(!workingSet.isEmpty());
		
		iteratedFrontiers.put(n, res);
	}
	
	private void computeFrontiers() {
		Iterator<N> it = topoSort();
		while(it.hasNext()) {
			N n = it.next();
			Set<N> df = frontiers.getNonNull(n);
			
			// DF(local)
			for(FastGraphEdge<N> e : graph.getEdges(n)) {
				// svar data isn't propagated across exception edges.
				// if(!(e instanceof TryCatchEdge)) {
					N succ = e.dst();
					if(idoms.get(succ) != n) {
						df.add(succ);
					}	
				// }
			}
			
			// DF(up)
			for(N forest : treeChildren.getNonNull(n)) {
				for(N forestFrontier : frontiers.getNonNull(forest)) {
					if(idoms.get(forestFrontier) != n) {
						df.add(forestFrontier);
					}
				}
			}
		}
	}
	
	private void computePreOrder() {
		for (N n : preOrder) {
			semiIndices.put(n, semiIndices.size());
			propagationMap.put(n, n);

			for (FastGraphEdge<N> succEdge : graph.getEdges(n)) {
				N succ = succEdge.dst();
				if(!semiIndices.containsKey(succ)) {
					parents.put(succ, n);
				}
			}
		}
	}
	
	private void computeDominators() {
		// ignore entry
		// i>0 to i > 2
		for(int i = preOrder.size() - 1; i > 0; i--) {
			N n = preOrder.get(i);
			N p = parents.get(n);
			
			int newIndex = semiIndices.get(n);
			for(FastGraphEdge<N> e : graph.getReverseEdges(n)) {
				N sd = calcSemiDom(e.src());
				newIndex = Math.min(newIndex, semiIndices.get(sd));
			}
			semiIndices.put(n, newIndex);
			
			N semiIndex = preOrder.get(newIndex);
			semiDoms.getNonNull(semiIndex).add(n);
			
			ancestors.put(n, p);
			
			for(N v : semiDoms.getNonNull(p)) {
				N u = calcSemiDom(v);
				if(semiIndices.get(u) < semiIndices.get(v)) {
					idoms.put(v, u);
				} else {
					idoms.put(v, p);
				}
			}
			
			semiDoms.get(p).clear();
		}
		
		for(int i=1; i < semiIndices.size(); i++) {
			N n = preOrder.get(i);
			if(idoms.get(n) != preOrder.get(semiIndices.get(n))) {
				idoms.put(n, idoms.get(idoms.get(n)));
			}
		}
	}
	
	private N calcSemiDom(N n) {
		propagate(n);
		return propagationMap.get(n);
	}
	
	private void propagate(N n) {
		Stack<N> wl = new Stack<>();
		wl.add(n);
		N anc = ancestors.get(n);
		
		while(ancestors.containsKey(anc)) {
			wl.push(anc);
			anc = ancestors.get(anc);
		}
		
		anc = wl.pop();
		
		N p = propagationMap.get(anc);
		int bottom = semiIndices.get(p);
		
		while(!wl.isEmpty()) {
			N d = wl.pop();
			int current = semiIndices.get(propagationMap.get(d));
			if(current > bottom) {
				propagationMap.put(d, propagationMap.get(anc));
			} else {
				bottom = current;
			}
			
			anc = d;
		}
	}
	
	public static class DominatorTree<N extends FastGraphVertex> extends FastDirectedGraph<N, FastGraphEdge<N>> {
		
	}
}
