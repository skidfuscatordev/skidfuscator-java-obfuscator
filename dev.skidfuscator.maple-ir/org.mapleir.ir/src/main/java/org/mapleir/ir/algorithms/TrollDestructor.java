package org.mapleir.ir.algorithms;

import org.mapleir.flowgraph.edges.DefaultSwitchEdge;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.SwitchEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.DefaultBlockFactory;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyPhiStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.BasicLocal;
import org.mapleir.ir.utils.CFGUtils;
import org.mapleir.stdlib.collections.bitset.BitSetIndexer;
import org.mapleir.stdlib.collections.bitset.GenericBitSet;
import org.mapleir.stdlib.collections.bitset.IncrementalBitSetIndexer;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.mapleir.stdlib.collections.map.ValueCreator;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.Map.Entry;

import static org.mapleir.ir.code.Opcode.LOCAL_LOAD;

/**
 * A dank SSA destructor that translates out of SSA by literally evaluating phi statements, lol
 */
public class TrollDestructor {

	private final ControlFlowGraph cfg;
	private final LocalsPool locals;
	private SSABlockLivenessAnalyser liveness;
	private SSADefUseMap defuse;

	private final NullPermeableHashMap<Local, GenericBitSet<Local>> interfere;
	private final NullPermeableHashMap<Local, GenericBitSet<Local>> pccs;
	private final PhiResBitSetFactory phiResSetCreator;
	private final NullPermeableHashMap<PhiResource, GenericBitSet<PhiResource>> unresolvedNeighborsMap;
	private final NullPermeableHashMap<BasicBlock, GenericBitSet<BasicBlock>> succsCache;
	private final GenericBitSet<PhiResource> candidateResourceSet;

	private TrollDestructor(ControlFlowGraph cfg) {
		this.cfg = cfg;
		locals = cfg.getLocals();
		interfere = new NullPermeableHashMap<>(locals);
		pccs = new NullPermeableHashMap<>(locals);
		phiResSetCreator = new PhiResBitSetFactory();
		unresolvedNeighborsMap = new NullPermeableHashMap<>(phiResSetCreator);
		defuse = new SSADefUseMap(cfg);
		defuse.compute();
		succsCache = new NullPermeableHashMap<>(key -> {
			GenericBitSet<BasicBlock> succs = cfg.createBitSet();
			cfg.getEdges(key).stream().map(e -> e.dst()).forEach(succs::add);
			return succs;
		});
		candidateResourceSet = phiResSetCreator.create();
	}

	public static void leaveSSA(ControlFlowGraph cfg) {
		TrollDestructor dest = new TrollDestructor(cfg);
		dest.init();
		// dest.writer.setName("destruct-init").export();

		dest.csaa_iii();
		// dest.writer.setName("destruct-cssa").export();

		// coalesce messes up the troll
		// dest.coalesce();
		// dest.writer.setName("destruct-coalesce").export();

		dest.leaveSSA();
		// dest.writer.setName("destruct-final").export();
	}

	// ============================================================================================================= //
	// =============================================== Initialization ============================================== //
	// ============================================================================================================= //
	private void init() {
		// init pccs
		for (CopyPhiStmt copyPhi : defuse.phiDefs.values()) {
			Local phiTarget = copyPhi.getVariable().getLocal();
			pccs.getNonNull(phiTarget).add(phiTarget);
//			System.out.println("Initphi " + phiTarget);
			for (Entry<BasicBlock, Expr> phiEntry : copyPhi.getExpression().getArguments().entrySet()) {
				if (phiEntry.getValue().getOpcode() != LOCAL_LOAD)
					throw new IllegalArgumentException("Phi arg is not local; instead is " + phiEntry.getValue().getClass().getSimpleName());
				Local phiSource = ((VarExpr) phiEntry.getValue()).getLocal();
				pccs.getNonNull(phiSource).add(phiSource);
//				System.out.println("Initphi " + phiSource);
			}
		}
//		System.out.println();

		// compute liveness
		(liveness = new SSABlockLivenessAnalyser(cfg)).compute();
//		writer.add("liveness", new LivenessDecorator<ControlFlowGraph, BasicBlock, FlowEdge<BasicBlock>>().setLiveness(liveness));

		buildInterference();
	}

	private void buildInterference() {
		for (BasicBlock b : cfg.vertices()) {
			GenericBitSet<Local> in = liveness.in(b); // not a copy!
			GenericBitSet<Local> out = liveness.out(b); // not a copy!

			// in interfere in
			for (Local l : in)
				interfere.getNonNull(l).addAll(in);

			// out interfere out
			for (Local l : out)
				interfere.getNonNull(l).addAll(out);

			// backwards traverse for dealing with variables that are defined and used in the same block
			GenericBitSet<Local> intraLive = out.copy();
			ListIterator<Stmt> it = b.listIterator(b.size());
			while (it.hasPrevious()) {
				Stmt stmt = it.previous();
				if (stmt instanceof CopyVarStmt) {
					CopyVarStmt copy = (CopyVarStmt) stmt;
					Local defLocal = copy.getVariable().getLocal();
					intraLive.remove(defLocal);
				}
				for (Expr child : stmt.enumerateOnlyChildren()) {
					if (stmt.getOpcode() == LOCAL_LOAD) {
						Local usedLocal = ((VarExpr) child).getLocal();
						if (intraLive.add(usedLocal)) {
							interfere.getNonNull(usedLocal).addAll(intraLive);
							for (Local l : intraLive)
								interfere.get(l).add(usedLocal);
						}
					}
				}
			}
		}

//		System.out.println("Interference:");
//		for (Entry<Local, GenericBitSet<Local>> entry : interfere.entrySet())
//			System.out.println("  " + entry.getKey() + " : " + entry.getValue());
//		System.out.println();
	}

	// ============================================================================================================= //
	// =================================================== CSSA ==================================================== //
	// ============================================================================================================= //
	private void csaa_iii() {
		// iterate over each phi expression
		for (Entry<Local, CopyPhiStmt> entry : defuse.phiDefs.entrySet()) {
//			System.out.println("process phi " + entry.getValue());

			Local phiTarget = entry.getKey(); // x0
			CopyPhiStmt copy = entry.getValue();
			BasicBlock defBlock = defuse.defs.get(phiTarget); // l0
			PhiExpr phi = copy.getExpression();
			candidateResourceSet.clear();
			unresolvedNeighborsMap.clear();

			// Initialize phiResources set for convenience
			final GenericBitSet<PhiResource> phiResources = phiResSetCreator.create();
			phiResources.add(new PhiResource(defBlock, phiTarget, true));
			for (Entry<BasicBlock, Expr> phiEntry : phi.getArguments().entrySet())
				phiResources.add(new PhiResource(phiEntry.getKey(), ((VarExpr) phiEntry.getValue()).getLocal(), false));

			// Determine what copies are needed using the four cases.
			handleInterference(phiResources);

			// Process unresolved resources
			resolveDeferred();

//			System.out.println("  Cand: " + candidateResourceSet);
			// Resolve the candidate resources
			Type phiType = phi.getType();
			for (PhiResource toResolve : candidateResourceSet) {
				if (toResolve.isTarget)
					resolvePhiTarget(toResolve, phiType);
				else for (Entry<BasicBlock, Expr> phiArg : phi.getArguments().entrySet()) {
					VarExpr phiVar = (VarExpr) phiArg.getValue();
					if (phiVar.getLocal() == toResolve.local)
						phiVar.setLocal(resolvePhiSource(toResolve.local, phiArg.getKey(), phiType));
				}
			}
//			System.out.println("  interference: ");
//			for (Entry<Local, GenericBitSet<Local>> entry2 : interfere.entrySet())
//				System.out.println("    " + entry2.getKey() + " : " + entry2.getValue());
//			System.out.println("  post-inserted: " + copy);

			// Merge pccs for all locals in phi
			final GenericBitSet<Local> phiLocals = locals.createBitSet();
			phiLocals.add(copy.getVariable().getLocal());
			for (Entry<BasicBlock, Expr> phiEntry : phi.getArguments().entrySet())
				phiLocals.add(((VarExpr) phiEntry.getValue()).getLocal());
			for (Local phiLocal : phiLocals)
				pccs.put(phiLocal, phiLocals);

			// Nullify singleton pccs
			for (GenericBitSet<Local> pcc : pccs.values())
				if (pcc.size() <= 1)
					pcc.clear();

//			System.out.println("  pccs:");
//			for (Entry<Local, GenericBitSet<Local>> entry2 : pccs.entrySet())
//				System.out.println("    " + entry2.getKey() + " : " + entry2.getValue());
//			System.out.println();
		}
	}

	// TODO: convert <BasicBlock, Local> into some sort of a phi resource struct
	private void handleInterference(GenericBitSet<PhiResource> phiLocals) {
		for (PhiResource resI : phiLocals) {
			GenericBitSet<Local> liveOutI = liveness.out(resI.block);
			GenericBitSet<Local> pccI = pccs.get(resI.local);
			for (PhiResource resJ : phiLocals) {
				GenericBitSet<Local> pccJ = pccs.get(resJ.local);
				if (!intersects(pccI, pccJ))
					continue;
				GenericBitSet<Local> liveOutJ = liveness.out(resJ.block);

				boolean piljEmpty = pccI.intersect(liveOutJ).isEmpty();
				boolean pjliEmpty = pccJ.intersect(liveOutI).isEmpty();
				if (piljEmpty ^ pjliEmpty) {
					// case 1 and 2 - handle it asymetrically for the necessary local
					candidateResourceSet.add(piljEmpty ? resJ : resI);
				} else if (piljEmpty & pjliEmpty) {
					// case 4 - reflexively update unresolvedNeighborsMap
					unresolvedNeighborsMap.getNonNull(resI).add(resJ);
					unresolvedNeighborsMap.getNonNull(resJ).add(resI);
				} else {
					// case 3 - handle it symetrically for both locals
					candidateResourceSet.add(resI);
					candidateResourceSet.add(resJ);
				}
			}
		}
	}

	private void resolveDeferred() {
		while (!unresolvedNeighborsMap.isEmpty()) {
			// Pick up resources in value of decreasing size
			PhiResource largest = null;
			int largestCount = 0;
			for (Entry<PhiResource, GenericBitSet<PhiResource>> entry : unresolvedNeighborsMap.entrySet()) {
				PhiResource x = entry.getKey();
				GenericBitSet<PhiResource> neighbors = entry.getValue();
				int size = neighbors.size();
				if (size > largestCount) {
					if (!candidateResourceSet.contains(x) && !neighbors.containsAll(candidateResourceSet)) {
						largestCount = size;
						largest = x;
					}
				}
			}

			if (largestCount > 0) {
//				System.out.println("  Add " + largest + " by case 4");
				candidateResourceSet.add(largest);
				unresolvedNeighborsMap.remove(largest);
			} else {
				break;
			}
		}
	}

	private boolean intersects(GenericBitSet<Local> pccI, GenericBitSet<Local> pccJ) {
		for (Local yi : pccI)
			for (Local yj : pccJ) // this right here is the reason mr. boissinot roasted you 10 years later
				if (interfere.get(yi).contains(yj))
					return true;
		return false;
	}

	private void resolvePhiTarget(PhiResource res, Type phiType) {
		Local spill = insertStart(res, phiType); // Insert spill copy

		// Update liveness
		GenericBitSet<Local> liveIn = liveness.in(res.block);
		liveIn.remove(res.local);
		liveIn.add(spill);

		// Reflexively update interference
		interfere.getNonNull(spill).addAll(liveIn);
		for (Local l : liveIn)
			interfere.get(l).add(spill);
	}

	// replace the phi target xi with xi' and place a temp copy xi = xi' after all phi statements.
	private Local insertStart(PhiResource res, Type type) {
		BasicBlock li = res.block;
		Local xi = res.local;
		if(li.isEmpty())
			throw new IllegalStateException("Trying to resolve phi target interference in empty block " + li);

		Local spill = locals.makeLatestVersion(xi);
		int i;
		for (i = 0; i < li.size() && li.get(i).getOpcode() == Opcode.PHI_STORE; i++) {
			CopyPhiStmt copyPhi = (CopyPhiStmt) li.get(i);
			VarExpr copyTarget = copyPhi.getVariable();
			if (copyTarget.getLocal() == xi)
				copyTarget.setLocal(spill);
		}
		li.add(i, new CopyVarStmt(new VarExpr(xi, type), new VarExpr(spill, type)));
		return spill;
	}

	private Local resolvePhiSource(Local xi, BasicBlock lk, Type phiType) {
		// Insert spill copy
		Local spill = insertEnd(xi, lk, phiType);

		// Update liveness
		GenericBitSet<Local> liveOut = liveness.out(lk);
		liveOut.add(spill);
		 // xi can be removed from liveOut iff it isn't live into any succ or used in any succ phi.
		for (BasicBlock lj : succsCache.getNonNull(lk)) {
			if (!liveness.in(lj).contains(xi)) removeFromOut: {
				for (int i = 0; i < lj.size() && lj.get(i).getOpcode() == Opcode.PHI_STORE; i++)
					if (((VarExpr) ((CopyPhiStmt) lj.get(i)).getExpression().getArguments().get(lk)).getLocal() == xi)
						break removeFromOut;
				liveOut.remove(xi); // poor man's for-else loop
			}
		}

		// Reflexively update interference
		interfere.getNonNull(spill).addAll(liveOut);
		for (Local l : liveOut)
			interfere.get(l).add(spill);

		return spill;
	}

	private Local insertEnd(Local xi, BasicBlock lk, Type type) {
		Local spill = locals.makeLatestVersion(xi);
		CopyVarStmt newCopy = new CopyVarStmt(new VarExpr(spill, type), new VarExpr(xi, type));
		if(lk.isEmpty())
			lk.add(newCopy);
		else if(!lk.get(lk.size() - 1).canChangeFlow())
			lk.add(newCopy);
		else
			lk.add(lk.size() - 1, newCopy);
		return spill;
	}

	// ============================================================================================================= //
	// ================================================== Coalescing =============================================== //
	// ============================================================================================================= //
	private void coalesce() {
		for (BasicBlock b : cfg.vertices()) {
			for (Iterator<Stmt> it = b.iterator(); it.hasNext(); ) {
				Stmt stmt = it.next();
				if (stmt instanceof CopyVarStmt) {
					CopyVarStmt copy = (CopyVarStmt) stmt;
//					System.out.println("check " + copy);
					if (checkCoalesce(copy)) {
//						System.out.println("  coalescing");
						it.remove(); // Remove the copy

						// Merge pccs
						GenericBitSet<Local> pccX = pccs.get(copy.getVariable().getLocal());
						Local localY = ((VarExpr) copy.getExpression()).getLocal();
						GenericBitSet<Local> pccY = pccs.get(localY);
						pccX.add(localY);
						pccX.addAll(pccY);
						for (Local l : pccY)
							pccs.put(l, pccX);
					}
				}
			}
		}

//		System.out.println("post-coalsce pccs:");
//		for (Entry<Local, GenericBitSet<Local>> entry : pccs.entrySet())
//			System.out.println("  " + entry.getKey() + " : " + entry.getValue());
//		System.out.println();
	}

	private boolean checkCoalesce(CopyVarStmt copy) {
		// Only coalesce simple copies x=y.
		if (copy.isSynthetic() || copy.getExpression().getOpcode() != LOCAL_LOAD)
			return false;

		Local localX = copy.getVariable().getLocal();
		Local localY = ((VarExpr) copy.getExpression()).getLocal();
		GenericBitSet<Local> pccX = pccs.getNonNull(localX), pccY = pccs.getNonNull(localY);

		// Trivial case: Now that we are in CSSA, we can simply drop copies within the same pcc.
		if (pccX == pccY)
			return true;

		boolean xEmpty = pccX.isEmpty(), yEmpty = pccY.isEmpty();
		// Case 1 - If pcc[x] and pcc[y] are empty the copy can be removed regardless of interference.
		if (xEmpty & yEmpty)
			return true;

		// Case 2 - If one of pcc[x] is not empty but pcc[y] is empty then the copy can removed if y does not
		// interfere with any local in (pcc[x]-x).
		else if (xEmpty ^ yEmpty) {
			if (checkPccSingle(yEmpty ? pccX : pccY, yEmpty ? localX : localY, yEmpty ? localY : localX))
				return false;
		}

		// Case 3 - If neither pcc[x] nor pcc[y] are empty, then the copy can be removed iff no local in pcc[y]
		// interferes with any local in (pcc[x]-x) and not local in pcc[x] interferes with any local in (pcc[y]-y).
		else if (checkPccDouble(pccX, localX, pccY, localY))
			return false;

		// No interference, copy can be removed
		return true;
	}

	// Returns true if the copy cannot be removed.
	private boolean checkPccSingle(GenericBitSet<Local> pccX, Local x, Local y) {
//		System.out.println("  case 2");
		GenericBitSet<Local> nonEmpty = pccX.copy();
		nonEmpty.remove(x);
		return interfere.getNonNull(y).containsAny(nonEmpty);
	}

	// Quadratic (lmao) coalesce check for coalesce case 3, returns true if the copy cannot be removed.
	// But thanks to bitsets its linear time
	private boolean checkPccDouble(GenericBitSet<Local> pccX, Local x, GenericBitSet<Local> pccY, Local y) {
//		System.out.println("  case 3");
		GenericBitSet<Local> pccYTrim = pccY.copy();
		pccYTrim.remove(y);
		for (Local lx : pccX)
			if (interfere.getNonNull(lx).containsAny(pccYTrim))
				return true;

		GenericBitSet<Local> pccXTrim = pccX.copy();
		pccXTrim.remove(x);
		for (Local ly : pccY)
			if (interfere.getNonNull(ly).containsAny(pccXTrim))
				return true;

		return false;
	}

	// ============================================================================================================= //
	// ================================================== Leave SSA ================================================ //
	// ============================================================================================================= //
	private void leaveSSA() {
		// so, instead of opting for the sane algorithm of flattening pccs and dropping phis, we are going to
		// literally evaluate the phi statements by checking which block we came from.
		Local predLocal = locals.getNextFreeLocal(false);
		VarExpr predVar = new VarExpr(predLocal, Type.INT_TYPE);
		for (BasicBlock b : cfg.vertices()) {
			Stmt newCopy = new CopyVarStmt(predVar.copy(), new ConstantExpr(b.getNumericId()));
			if(b.get(b.size() - 1).canChangeFlow())
				b.add(b.size() - 1, newCopy);
			else
				b.add(newCopy);
		}

		// expand phis into switches based on predecessor (LOL)
		for (BasicBlock b : new ArrayList<>(cfg.vertices())) {
			for (ListIterator<Stmt> it = b.listIterator(); it.hasNext(); ) {
				Stmt stmt = it.next();
				if (stmt instanceof CopyPhiStmt) {
					it.remove();
					CopyPhiStmt phi = (CopyPhiStmt) stmt;
					BasicBlock splitBlock = CFGUtils.splitBlock(DefaultBlockFactory.INSTANCE, cfg, b, it.previousIndex());
					Set<FlowEdge<BasicBlock>> splitEdges = cfg.getEdges(splitBlock);
					assert(splitEdges.size() == 1);
					cfg.removeEdge(splitEdges.iterator().next());
					LinkedHashMap<Integer, BasicBlock> dsts = new LinkedHashMap<>();
					for (Entry<BasicBlock, Expr> phiArg : phi.getExpression().getArguments().entrySet()) {
						BasicBlock stubBlock = new BasicBlock(cfg);
						// i don't think it's necessary to copy phi arg expression since we
						// are just reassigning it to a different block. tricky!
						stubBlock.add(new CopyVarStmt(phi.getVariable().copy(), phiArg.getValue()));
						final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(stubBlock, b);
						stubBlock.add(new UnconditionalJumpStmt(b, edge));
						cfg.addEdge(edge);
						int predId = phiArg.getKey().getNumericId();
						dsts.put(predId, stubBlock);
						cfg.addEdge(new SwitchEdge<>(splitBlock, stubBlock, predId));
					}

					splitBlock.add(new SwitchStmt(predVar.copy(), dsts, splitBlock));
					cfg.addEdge(new DefaultSwitchEdge<>(splitBlock, splitBlock));
				} else break;
			}
		}

		// Flatten pccs into one variable through remapping
		// System.out.println("remap:");
		Map<Local, Local> remap = new HashMap<>();
		for (Entry<Local, GenericBitSet<Local>> entry : pccs.entrySet()) {
			GenericBitSet<Local> pcc = entry.getValue();
			if (pcc.isEmpty())
				continue;

			Local local = entry.getKey();
			if (remap.containsKey(local))
				continue;

			BasicLocal newLocal = locals.get(locals.getMaxLocals() + 1, false);
			// System.out.println("  " + local + " -> " + newLocal);
			remap.put(local, newLocal);
			for (Local pccLocal : pcc) {
				if (remap.containsKey(pccLocal))
					continue;
				newLocal = locals.get(locals.getMaxLocals() + 1, false);
				remap.put(pccLocal, newLocal);
				// System.out.println("  " + pccLocal + " -> " + newLocal);
			}
		}
		System.out.println();

		for (BasicBlock b : new ArrayList<>(cfg.vertices())) {
			for (Stmt stmt : b) {
				// Apply remappings
				if (stmt instanceof CopyVarStmt) {
					VarExpr lhs = ((CopyVarStmt) stmt).getVariable();
					Local copyTarget = lhs.getLocal();
					lhs.setLocal(remap.getOrDefault(copyTarget, copyTarget));
				}
				for (Expr child : stmt.enumerateOnlyChildren()) {
					if (child.getOpcode() == LOCAL_LOAD) {
						VarExpr var = (VarExpr) child;
						Local loadSource = var.getLocal();
						var.setLocal(remap.getOrDefault(loadSource, loadSource));
					}
				}
			}
		}

//		System.out.println();
	}

	// ============================================================================================================= //
	// =================================================== Structs ================================================= //
	// ============================================================================================================= //
	private class PhiResource {
		BasicBlock block;
		Local local;
		boolean isTarget;

		public PhiResource(BasicBlock block, Local local, boolean isTarget) {
			this.local = local;
			this.block = block;
			this.isTarget = isTarget;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			PhiResource that = (PhiResource) o;

			if (!local.equals(that.local))
				return false;
			return block.equals(that.block);

		}

		@Override
		public int hashCode() {
			int result = local.hashCode();
			result = 31 * result + block.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return block.getDisplayName() + ":" + local + (isTarget? "(targ)" : "");
		}
	}

	private class PhiResBitSetFactory implements ValueCreator<GenericBitSet<PhiResource>> {
		private final BitSetIndexer<PhiResource> phiResIndexer;

		PhiResBitSetFactory() {
			phiResIndexer = new IncrementalBitSetIndexer<>();
		}

		@Override
		public GenericBitSet<PhiResource> create() {
			return new GenericBitSet<>(phiResIndexer);
		}
	}
}
