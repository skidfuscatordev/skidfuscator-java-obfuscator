package org.mapleir.ir.algorithms;

import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.locals.Local;
import org.mapleir.stdlib.collections.bitset.GenericBitSet;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.algorithms.ExtendedDfs;
import org.mapleir.stdlib.collections.graph.algorithms.LT79Dom;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;

import java.util.List;
import java.util.Set;

/**
 * A fast dominance-based liveness analyser for SSA-form flow graphs.
 * This class implements the algorithm described in the 2007 paper "Fast Liveness Checking
 * for SSA-Form Programs" by Boissinot et al..
 * @see <a href="https://hal.inria.fr/inria-00192219/file/fast_liveness.pdf">Fast
 * Liveness Checking for SSA-Form Programs</a>
 */
public class DominanceLivenessAnalyser {

	private final NullPermeableHashMap<BasicBlock, GenericBitSet<BasicBlock>> rv;
	private final NullPermeableHashMap<BasicBlock, GenericBitSet<BasicBlock>> tq;

	public final ControlFlowGraph cfg;
	private SSADefUseMap defuse;
	public final ControlFlowGraph reducedCfg;
	public final ExtendedDfs<BasicBlock> dfs;
	public final NullPermeableHashMap<BasicBlock, GenericBitSet<BasicBlock>> backEdges;
	public final GenericBitSet<BasicBlock> backTargets;
	public final LT79Dom<BasicBlock, FlowEdge<BasicBlock>> domc;
	
	private final List<BasicBlock> preOrder, postOrder;
	private final NullPermeableHashMap<BasicBlock, GenericBitSet<BasicBlock>> sdoms;

	public static int k1, k2;
	public DominanceLivenessAnalyser(ControlFlowGraph cfg, BasicBlock entry, SSADefUseMap defuse) {
		this.cfg = cfg;
		this.defuse = defuse;

		rv = new NullPermeableHashMap<>(cfg);
		tq = new NullPermeableHashMap<>(cfg);
		sdoms = new NullPermeableHashMap<>(cfg);

		dfs = new ExtendedDfs<>(cfg, ExtendedDfs.EDGES).run(entry);
		backEdges = new NullPermeableHashMap<>(cfg);
		backTargets = cfg.createBitSet();
		reducedCfg = reduce(cfg, dfs.getEdges(ExtendedDfs.BACK));

		domc = new LT79Dom<>(cfg, entry);
		preOrder = domc.getPreOrder();
		postOrder = domc.getPostOrder();
		
		computeStrictDominators(domc);
		
		computeReducedReachability();
		computeTargetReachability();

//		for(BasicBlock b : cfg.vertices()) {
//			Set<BasicBlock> other = new HashSet<>(sdoms.getNonNull(b));
//			other.add(b);
//			if(!other.equals(domc2.getDominates(b))) {
//				System.out.println(domc2.getDominates(b));
//				System.out.println(other);
//				throw new RuntimeException();
//			}
//		}
	}

	public void setDefuse(SSADefUseMap defuse) {
		this.defuse = defuse;
	}

	private void computeStrictDominators(LT79Dom<BasicBlock, FlowEdge<BasicBlock>> dom) {
		for (BasicBlock b : postOrder) {
			Set<BasicBlock> set = sdoms.getNonNull(b);
			set.addAll(dom.getDominates(b));
			set.remove(b);
		}
	}

	private void computeReducedReachability() {
		for (BasicBlock b : postOrder) {
			rv.getNonNull(b).add(b);
			for (FlowEdge<BasicBlock> e : reducedCfg.getReverseEdges(b)) {
				rv.getNonNull(e.src()).addAll(rv.get(b));
			}
		}
	}

	private void computeTargetReachability() {
		for (BasicBlock b : preOrder) {
			tq.getNonNull(b).add(b);

			// Tup(t) = set of unreachable backedge targets from reachable sources
			GenericBitSet<BasicBlock> tup = backEdges.getNonNull(b).relativeComplement(rv.get(b));
			for (BasicBlock w : tup) {
				tq.get(b).addAll(tq.get(w));
			}
		}
	}

	private ControlFlowGraph reduce(ControlFlowGraph cfg, Set<FastGraphEdge<BasicBlock>> back) {
		ControlFlowGraph reducedCfg = cfg.copy();
		for (FastGraphEdge<BasicBlock> e : back) {
			reducedCfg.removeEdge((FlowEdge<BasicBlock>) e);
			backTargets.add(e.dst());
			backEdges.getNonNull(e.src()).add(e.dst());
		}
		return reducedCfg;
	}

	public boolean isLiveIn(BasicBlock b, Local l) {
		BasicBlock defBlock = defuse.defs.get(l);

		if (defuse.phiDefs.containsKey(l) && defBlock == b) {
			return true;
		}

		GenericBitSet<BasicBlock> tqa = tq.get(b).intersect(sdoms.getNonNull(defBlock));
		for (BasicBlock t : tqa) {
			GenericBitSet<BasicBlock> rt = rv.get(t).intersect(defuse.uses.get(l));
			if (!rt.isEmpty())
				return true;
		}

		return false;
	}

	public boolean isLiveOut(BasicBlock q, Local a) {
		BasicBlock defBlock = defuse.defs.get(a);

		GenericBitSet<BasicBlock> uses = defuse.uses.getNonNull(a);
		if (defBlock == q) {
			return !uses.relativeComplement(defBlock).isEmpty() || defuse.phiUses.get(defBlock).contains(a);
		}

		boolean targ = !backTargets.contains(q);

		GenericBitSet<BasicBlock> sdomdef = sdoms.getNonNull(defBlock);
		if (sdomdef.contains(q)) {
			GenericBitSet<BasicBlock> tqa = tq.get(q).intersect(sdomdef);

			for (BasicBlock t : tqa) {
				GenericBitSet<BasicBlock> u = uses.copy();
				if (t == q && targ)
					u.remove(q);

				GenericBitSet<BasicBlock> rtt = rv.getNonNull(t).intersect(u);
				if (!rtt.isEmpty())
					return true;
			}
		}

		return false;
	}
}
