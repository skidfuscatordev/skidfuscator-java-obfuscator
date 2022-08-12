package org.mapleir.stdlib.collections.graph.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphEdgeImpl;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.mapleir.stdlib.collections.map.SetCreator;

/**
 * Implementation of A Fast Algorithm for Finding Dominators in a Flowgraph
 * by Lengauer and Tarjan, 1979
 */
public class LT79Dom<N extends FastGraphVertex, E extends FastGraphEdge<N>> {

	private final FastDirectedGraph<N, E> graph;
	private final N root;
	private final boolean computeFrontiers;
	
	/* semi(w)=
	 *    (i) before semidominators are computed: vertex(w)
	 *   (ii) after semidominators are computed: the semidominator of w */
	private final Map<N, Integer> semi;
	/* vertex(i) = vertex with dfs pre-time == i*/
	private final Map<Integer, N> vertex;
	private final List<N> postOrder;
	/* parent(w) = parent of w in the dfs spanning tree */
	private final Map<N, N> parent;
	/* see step 3, idom(w) == immediate dominator of w after step 4 */
	private final Map<N, N> idoms;
	/* bucket(w) = set of vertices whose semidominator is w */
	private final NullPermeableHashMap<N, Set<N>> bucket;
	private final Map<N, N> ancestor;
	private final Map<N, N> label;
	
	/* treeDescendants(n) = descendants of n in the dominator tree + n */
	private final NullPermeableHashMap<N, Set<N>> treeDescendants;
	/* treeSuccessors(n) = direct successors of n in the */
	private final NullPermeableHashMap<N, Set<N>> treeSuccessors;
	/* graph representation of the dominator tree. edges are of
	 * the form(idom(n), n) */
	private final DominatorTree<N> dominatorTree;
	
	private final NullPermeableHashMap<N, Set<N>> frontiers;
	private final NullPermeableHashMap<N, Set<N>> iteratedFrontiers;

	public LT79Dom(FastDirectedGraph<N, E> graph, N root) {
		this(graph, root, true);
	}
	
	public LT79Dom(FastDirectedGraph<N, E> graph, N root, boolean computeFrontiers) {
		this.graph = graph;
		this.root = root;
		
		semi = new HashMap<>();
		vertex = new HashMap<>();
		postOrder = new ArrayList<>();
		parent = new HashMap<>();
		idoms = new HashMap<>();
		bucket = new NullPermeableHashMap<>(new SetCreator<>());
		ancestor = new HashMap<>();
		label = new HashMap<>();
		
		treeDescendants = new NullPermeableHashMap<>(new SetCreator<>());
		treeSuccessors = new NullPermeableHashMap<>(new SetCreator<>());
		frontiers = new NullPermeableHashMap<>(new SetCreator<>());
		iteratedFrontiers = new NullPermeableHashMap<>(new SetCreator<>());
		
		step1();
		/* carry out step 2 and 3 on all w != r ∈ V in decreasing order by
		 * number. maintain a forest of vertex set V and edge set 
		 * {(parent(w), w) | w ∈ processed_nodes()} */
		step2and3();
		/* step 4 examines vertices in increasing order by number, filling in
		 * the immediate dominators not explicitly computed in step 3. */
		step4();
		
		dominatorTree = makeDominatorTree();
		this.computeFrontiers = computeFrontiers;
		if(computeFrontiers) {
			dfrontiers();
			iteratedFrontiers();
		}
	}
	
	public List<N> getPreOrder() {
		List<N> preOrder = new ArrayList<>();
		for(int i=0; i < vertex.size(); i++) {
			preOrder.add(vertex.get(i));
		}
		return preOrder;
	}
	
	public List<N> getPostOrder() {
		return new ArrayList<>(postOrder);
	}
	
	private void step1() {
		dfs(root);
		
		assert vertex.get(0) == root;
	}
	
	private void dfs(N v) {
		int n = semi.size();
		semi.put(v, n);
		vertex.put(n, v);
		ancestor.put(v, null);
		label.put(v, v);
		
		for(E succ : graph.getEdges(v)) {
			N w = succ.dst();
			if(!semi.containsKey(w)) {
				parent.put(w, v);
				dfs(w);
			}
		}
		
		postOrder.add(v);
	}
	
	private void step2and3() {
		/* ignore entry */
		for(int i=semi.size() - 1; i > 0; i--) {
			N w = vertex.get(i);
			step2(w);
			step3(w);
		}
	}
	
	private void step2(N w) {
		/* Theorem 4: For any vertex w != r:
		 *   sdom(w) = min({v | (v, w} ∈ E and v < w} ∪ 
		 *   {sdom(u) |u > w and ∃(v, w) such that u ->* v}) 
		 */
		for(E pred : graph.getReverseEdges(w)) {
			N v = pred.src();
			N u = eval(v);
			if(semi.get(u) < semi.get(w)) {
				semi.put(w, semi.get(u));
			}
		}
		bucket.getNonNull(vertex.get(semi.get(w))).add(w);
		link(parent.get(w), w);
	}
	
	private void step3(N w) {
		/* Corollary 1: Let w != r and let u be a vertex for which sdom(u) is a
		 * minimum among vericies u satisfying sdom(w) ->+ u ->* w, then:
		 *   idom(w) = sdom(w); if sdom(w) == sdom(u) 
		 *   idom(w) = idom(u); otherwise
		 * 
		 * implicitly define the immediate dominator of each vertex by applying
		 * corollary 1. */
		Set<N> wbucket = bucket.getNonNull(parent.get(w));
		for(N v : wbucket) {
			N u = eval(v);
			/* If the semidominator of w is its immediate dominator, then dom is
			 * the immediate dominator of w. Otherwise dom is a vertex, v, whose
			 * number is smaller than w and whose immediate dominator is also
			 * w's immediate dominator. */
			N dom = semi.get(u) < semi.get(v) ? u : parent.get(w);
			idoms.put(v, dom);
		}
		wbucket.clear();
	}
	
	private void step4() {
		/* explicitly define the immediate dominator of each vertex, carrying
		 * out the computation vertex by vertex in increasing order by
		 * number. */
		for(int i=0; i < semi.size(); i++) {
			N w = vertex.get(i);
			if(idoms.get(w) != vertex.get(semi.get(w))) {
				idoms.put(w, idoms.get(idoms.get(w)));
			}
		}
	}
	
	/* add (v, w) to the forest */
	private void link(N v, N w) {
		ancestor.put(w, v);
	}
	
	/* if v is a root of a tree in the forest, return v. else let r be the root
	 * of the tree in the forest which contains v. return any vertex u != r of
	 * minimum semi(u) on the path r ->* v */
	private N eval(N v) {
		if(ancestor.get(v) != null) {
			compress(v);
			return label.get(v);
		} else {
			return v;
		}
	}
	
	private void compress(N v) {
		if (ancestor.get(ancestor.get(v)) != null) {
			compress(ancestor.get(v));
			if (semi.get(label.get(ancestor.get(v))) < semi.get(label.get(v))) {
				label.put(v, label.get(ancestor.get(v)));
			}
			ancestor.put(v, ancestor.get(ancestor.get(v)));
		}
	}
	
	private void dfrontiers() {
		for(N n : treeReverseTopoOrder()) {
			Set<N> df = frontiers.getNonNull(n);
			// DF (local)
			for(E e : graph.getEdges(n)) {
				N succ = e.dst();
				if(idoms.get(succ) != n) {
					df.add(succ);
				}
			}
			// DF (up)
			for(N f : treeSuccessors.getNonNull(n)) {
				for(N ff : frontiers.getNonNull(f)) {
					if(idoms.get(ff) != n) {
						df.add(ff);
					}
				}
			}
		}
	}
	
	private List<N> treeReverseTopoOrder() {
		/* side notes: topo sort on tree == pre order on tree (not on general
		 * DAG) == reverse post order on general DAG,
		 * therefore reverse topo sort on tree == post order of general DAG */
		ExtendedDfs<N> dfs = new ExtendedDfs<>(getDominatorTree(),
				ExtendedDfs.POST);
		dfs.run(root);
		return dfs.getPostOrder();
	}
	
	private void iteratedFrontiers() {
		for(N n : postOrder) {
			iteratedFrontier(n);
		}
	}
	
	private void iteratedFrontier(N n) {
		Set<N> res = new HashSet<>();
		Set<N> workingSet = new HashSet<>();
		workingSet.add(n);
		
		do {
			Set<N> newWorkingSet = new HashSet<>();
			for(N n1 : workingSet) {
				for(N n2 : frontiers.get(n1)) {
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
	
	private DominatorTree<N> makeDominatorTree() {
		DominatorTree<N> tree = new DominatorTree<>();
		for(N v : postOrder) {
			N idom = idoms.get(v);
			if(idom != null) {
				Set<N> decs = treeDescendants.getNonNull(idom);
				decs.add(v);
				decs.addAll(treeDescendants.getNonNull(v));
				
				Set<N> succs = treeSuccessors.getNonNull(idom);
				succs.add(v);
				
				tree.addEdge(new FastGraphEdgeImpl<>(idom, v));
			}
			treeDescendants.getNonNull(v).add(v);
		}
		return tree;
	}
	
	public DominatorTree<N> getDominatorTree() {
		return dominatorTree;
	}
	
	public Set<N> getDominates(N v) {
		return new HashSet<>(treeDescendants.getNonNull(v));
	}
	
	public N getImmediateDominator(N v) {
		return idoms.get(v);
	}
	
	public Set<N> getDominanceFrontier(N v) {
		if(computeFrontiers) {
			return new HashSet<>(frontiers.getNonNull(v));
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	public Set<N> getIteratedDominanceFrontier(N v) {
		if(computeFrontiers) {
			return new HashSet<>(iteratedFrontiers.getNonNull(v));
		} else {
			throw new UnsupportedOperationException();
		}
	}
}
