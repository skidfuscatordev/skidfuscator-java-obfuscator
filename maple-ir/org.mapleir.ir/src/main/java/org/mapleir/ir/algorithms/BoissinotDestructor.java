package org.mapleir.ir.algorithms;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.code.stmt.copy.CopyPhiStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.mapleir.ir.utils.CFGUtils;
import org.mapleir.stdlib.collections.bitset.GenericBitSet;
import org.mapleir.stdlib.collections.graph.algorithms.SimpleDfs;
import org.mapleir.stdlib.collections.map.ListCreator;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.Map.Entry;

/**
 * An out-of-SSA implementation based on the 2009 paper "Revisiting Out-of-SSA
 * Translation for Correctness, CodeQuality, and Efficiency" by Boissinot et al..
 *
 * @see <a href="https://hal.inria.fr/inria-00349925v1/document">Revisiting
 * Out-of-SSA Translation for Correctness, CodeQuality, and Efficiency</a>
 *
 * @see <a href="https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.385.8510&rep=rep1&type=pdf">Boissinot's PhD thesis</a>
 * @see <a href="https://compilers.cs.uni-saarland.de/ssasem/talks/Alain.Darte.pdf">Slides</a>
 */
public class BoissinotDestructor {
	// private boolean DO_VALUE_INTERFERENCE = true;
	// private boolean DO_SHARE_COALESCE = true;
	
	public static void leaveSSA(ControlFlowGraph cfg) {
		new BoissinotDestructor(cfg);
	}

	private final ControlFlowGraph cfg;
	private final LocalsPool locals;
	private final BasicBlock entry;

	private final DominanceLivenessAnalyser resolver;
	private final NullPermeableHashMap<Local, LinkedHashSet<Local>> values;
	private final SimpleDfs<BasicBlock> dom_dfs;
	private final SSADefUseMap defuse;

	private final Map<Local, Local> equalAncIn;
	private final Map<Local, Local> equalAncOut;

	private final Map<Local, CongruenceClass> congruenceClasses;
	private final Map<Local, Local> remap;

	private BoissinotDestructor(ControlFlowGraph cfg) {
		this.cfg = cfg;
		locals = cfg.getLocals();

		// if ((flags & 1) != 0)
		// DO_VALUE_INTERFERENCE = true;
		// if ((flags & 2) != 0)
		// DO_SHARE_COALESCE = true;
		
		values = new NullPermeableHashMap<>(local -> {
			LinkedHashSet<Local> vc = new LinkedHashSet<>();
			vc.add(local);
			return vc;
		});
		congruenceClasses = new HashMap<>();
		equalAncIn = new HashMap<>();
		equalAncOut = new HashMap<>();
		remap = new HashMap<>();

		// 1. Insert copies to enter CSSA.
		liftPhiOperands();

		entry = CFGUtils.deleteUnreachableBlocks(cfg);

		// compute the dominance here after we have lifted non variable phi operands.
		resolver = new DominanceLivenessAnalyser(cfg, entry, null);

		copyPhiOperands();
		
		dom_dfs = traverseDominatorTree();
		defuse = createDuChains();
		// this is bad.
		resolver.setDefuse(defuse);

		computeValueInterference();

		coalescePhis();
		coalesceCopies();
		
		remapLocals();
		applyRemapping();

		sequentialize();
	}

	private void liftPhiOperands() {
		for (BasicBlock b : cfg.vertices()) {
			for (Stmt stmt : new ArrayList<>(b)) {
				if (stmt.getOpcode() == Opcode.PHI_STORE) {
					CopyPhiStmt copy = (CopyPhiStmt) stmt;
					for (Entry<BasicBlock, Expr> e : copy.getExpression().getArguments().entrySet()) {
						Expr expr = e.getValue();
						int opcode = expr.getOpcode();
						if (opcode == Opcode.CONST_LOAD || opcode == Opcode.CATCH) {
							VersionedLocal vl = locals.makeLatestVersion(locals.get(0, false));
							CopyVarStmt cvs = new CopyVarStmt(new VarExpr(vl, expr.getType()), expr);
							e.setValue(new VarExpr(vl, expr.getType()));

							insertEnd(e.getKey(), cvs);
						} else if (opcode != Opcode.LOCAL_LOAD) {
							throw new IllegalArgumentException("Non-variable expression in phi: " + copy);
						}
					}
				}
			}
		}
	}

	private void copyPhiOperands() {
		for (BasicBlock b : cfg.vertices()) {
			if (b != entry) {
				copyPhiOperands(b);
			}
		}
	}

	private void copyPhiOperands(BasicBlock b) {
		NullPermeableHashMap<BasicBlock, List<PhiRes>> wl = new NullPermeableHashMap<>(new ListCreator<>());
		ParallelCopyVarStmt dst_copy = new ParallelCopyVarStmt();

		// given a phi: L0: x0 = phi(L1:x1, L2:x2)
		// insert the copies:
		// L0: x0 = x3 (at the end of L0)
		// L1: x4 = x1
		// L2: x5 = x2
		// and change the phi to:
		// x3 = phi(L1:x4, L2:x5)

		for (Stmt stmt : b) {
			// phis only appear at the start of a block.
			if (stmt.getOpcode() != Opcode.PHI_STORE) {
				break;
			}

			CopyPhiStmt copy = (CopyPhiStmt) stmt;
			PhiExpr phi = copy.getExpression();

			// for every xi arg of the phi from pred Li, add it to the worklist
			// so that we can parallelise the copy when we insert it.
			for (Entry<BasicBlock, Expr> e : phi.getArguments().entrySet()) {
				BasicBlock h = e.getKey();
				// these are validated in init().
				VarExpr v = (VarExpr) e.getValue();
				PhiRes r = new PhiRes(copy.getVariable().getLocal(), phi, h, v.getLocal(), v.getType());
				wl.getNonNull(h).add(r);
			}

			// for each x0, where x0 is a phi copy target, create a new variable z0 for
			// a copy x0 = z0 and replace the phi copy target to z0.
			Local x0 = copy.getVariable().getLocal();
			Local z0 = locals.makeLatestVersion(x0);
			dst_copy.pairs.add(new CopyPair(x0, z0, copy.getVariable().getType())); // x0 = z0
			copy.getVariable().setLocal(z0); // z0 = phi(...)
		}

		// resolve
		if (dst_copy.pairs.size() > 0)
			insertStart(b, dst_copy);

		for (Entry<BasicBlock, List<PhiRes>> e : wl.entrySet()) {
			BasicBlock p = e.getKey();
			ParallelCopyVarStmt copy = new ParallelCopyVarStmt();

			for (PhiRes r : e.getValue()) {
				// for each xi source in a phi, create a new variable zi, and insert the copy
				// zi = xi in the pred Li. then replace the phi arg from Li with zi.

				Local xi = r.l;
				Local zi = locals.makeLatestVersion(xi);
				copy.pairs.add(new CopyPair(zi, xi, r.type));

				// we consider phi args to be used in the pred instead of the block
				// where the phi is, so we need to update the def/use maps here.
				r.phi.setArgument(r.pred, new VarExpr(zi, r.type));
			}

			insertEnd(p, copy);
		}
	}

	private void insertStart(BasicBlock b, Stmt copy) {
		if (b.isEmpty()) {
			b.add(copy);
		} else {
			// insert after phi.
			int i;
			for (i = 0; i < b.size() && b.get(i).getOpcode() == Opcode.PHI_STORE; i++)
				; // skipping ptr.
			b.add(i, copy);
		}
	}

	private void insertEnd(BasicBlock b, Stmt copy) {
		if (b.isEmpty()) {
			b.add(copy);
		} else {
			int pos = b.size() - 1;
			if (b.get(pos).canChangeFlow()) {
				b.add(pos, copy);
			} else {
				b.add(copy);
			}
		}
	}

	private SimpleDfs<BasicBlock> traverseDominatorTree() {
		return new SimpleDfs<>(resolver.domc.getDominatorTree(), entry, SimpleDfs.PRE | SimpleDfs.TOPO);
	}

	private SSADefUseMap createDuChains() {
		SSADefUseMap defuse = new SSADefUseMap(cfg) {
			@Override
			protected void build(BasicBlock b, Stmt stmt, Set<Local> usedLocals) {
				if (stmt instanceof ParallelCopyVarStmt) {
					ParallelCopyVarStmt copy = (ParallelCopyVarStmt) stmt;
					for (CopyPair pair : copy.pairs) {
						defs.put(pair.targ, b);
						uses.getNonNull(pair.source).add(b);
					}
				} else {
					super.build(b, stmt, usedLocals);
				}
			}

			@Override
			protected void buildIndex(BasicBlock b, Stmt stmt, int index, Set<Local> usedLocals) {
				if (stmt instanceof ParallelCopyVarStmt) {
					ParallelCopyVarStmt copy = (ParallelCopyVarStmt) stmt;
					for (CopyPair pair : copy.pairs) {
						defIndex.put(pair.targ, index);
						lastUseIndex.getNonNull(pair.source).put(b, index);
					}
				} else {
					super.buildIndex(b, stmt, index, usedLocals);
				}
				return;
			}
		};
		defuse.computeWithIndices(dom_dfs.getPreOrder());
		return defuse;
	}

	private void computeValueInterference() {
		List<BasicBlock> topoorder = dom_dfs.getTopoOrder();
		assert (topoorder.size() >= cfg.vertices().size());
		
		for (BasicBlock bl : topoorder) {
			for (Stmt stmt : bl) {
				int opcode = stmt.getOpcode();
				if (opcode == Opcode.LOCAL_STORE) {
					CopyVarStmt copy = (CopyVarStmt) stmt;
					Expr e = copy.getExpression();
					Local b = copy.getVariable().getLocal();

					// Expression has to be a VarExpr
					if (!copy.isSynthetic() && e.getOpcode() == Opcode.LOCAL_LOAD) {
						LinkedHashSet<Local> vc = values.get(((VarExpr) e).getLocal());
						vc.add(b);
						values.put(b, vc);
					} else {
						values.getNonNull(b);
					}
				} else if (opcode == Opcode.PHI_STORE) {
					CopyPhiStmt copy = (CopyPhiStmt) stmt;
					values.getNonNull(copy.getVariable().getLocal());
				} else if (opcode == ParallelCopyVarStmt.PARALLEL_STORE) {
					ParallelCopyVarStmt copy = (ParallelCopyVarStmt) stmt;
					for (CopyPair p : copy.pairs) {
						LinkedHashSet<Local> valueClass = values.getNonNull(p.source);
						valueClass.add(p.targ);
						values.put(p.targ, valueClass);
					}
				}
			}
		}
	}
	
	// Initialize ccs based on phis and drop phi statements
	private void coalescePhis() {
		GenericBitSet<BasicBlock> processed = cfg.createBitSet();

		for (Entry<Local, CopyPhiStmt> e : defuse.phiDefs.entrySet()) {
			Local l = e.getKey();
			BasicBlock b = e.getValue().getBlock();

			// since we are now in cssa, phi locals never interfere and are in the same congruence class.
			// therefore we can coalesce them all together and drop phis. with this, we leave cssa.
			PhiExpr phi = e.getValue().getExpression();

			CongruenceClass pcc = new CongruenceClass();
			pcc.add(l);
			congruenceClasses.put(l, pcc);

			for (Expr ex : phi.getArguments().values()) {
				VarExpr v = (VarExpr) ex;
				Local argL = v.getLocal();
				pcc.add(argL);
				congruenceClasses.put(argL, pcc);
			}

			// is b is null, this phi copy has block equal to `null`, i.e. it has already
			// been removed from its block, and thus, already has been processed, so we can skip it.
			// otherwise, we can simply drop all the phis in the block without further consideration
			if (b != null && !processed.contains(b)) {
				processed.add(b);
				Iterator<Stmt> it = b.iterator();
				while(it.hasNext()) {
					Stmt stmt = it.next();
					if(stmt.getOpcode() == Opcode.PHI_STORE) {
						it.remove();
					} else {
						break;
					}
				}
			}
		}

		defuse.phiDefs.clear();
	}

	// Coalesce parallel and standard copies based on value interference, dropping coalesced copies
	private void coalesceCopies() {
		// now for each copy check if lhs and rhs congruence classes do not interfere.
		// if they do not interfere merge the conClasses and those two vars can be coalesced. delete the copy.
		assert (dom_dfs.getPreOrder().size() >= cfg.vertices().size());
		for (BasicBlock b : dom_dfs.getPreOrder()) {
			for (Iterator<Stmt> it = b.iterator(); it.hasNext();) {
				Stmt stmt = it.next();
				if (stmt instanceof CopyVarStmt) {
					CopyVarStmt copy = (CopyVarStmt) stmt;
					if (!copy.isSynthetic() && copy.getExpression() instanceof VarExpr) {
						Local lhs = copy.getVariable().getLocal();
						Local rhs = ((VarExpr) copy.getExpression()).getLocal();
						if (!isReservedRegister((VersionedLocal) rhs)) {
							if (tryCoalesceCopyValue(lhs, rhs)) {
//								 System.out.println("COPYKILL(1) " + lhs + " == " + rhs);
								it.remove();
							}

							if (tryCoalesceCopySharing(lhs, rhs)) {
//								 System.out.println("SHAREKILL(1) " + lhs + " == " + rhs);
								it.remove();
							}
						}
					}
				} else if (stmt instanceof ParallelCopyVarStmt) {
					// we need to do it for each one. if all of the copies are
					// removed then remove the pcvs
					ParallelCopyVarStmt copy = (ParallelCopyVarStmt) stmt;
					for (Iterator<CopyPair> pairIter = copy.pairs.listIterator(); pairIter.hasNext();) {
						CopyPair pair = pairIter.next();
						Local lhs = pair.targ, rhs = pair.source;
						
						if(!isReservedRegister((VersionedLocal) rhs)) {
							if (tryCoalesceCopyValue(lhs, rhs)) {
								// System.out.println("COPYKILL(2) " + lhs + " == " + rhs);
								pairIter.remove();
							}

							if (tryCoalesceCopySharing(lhs, rhs)) {
								// System.out.println("SHAREKILL(2) " + lhs + " == " + rhs);
								pairIter.remove();
							}
						}
					}
					if (copy.pairs.isEmpty())
						it.remove();
				}
			}
		}
	}

	private boolean isReservedRegister(VersionedLocal l) {
		return locals.isReservedRegister(l);
	}

	// Process the copy a = b. Returns true if a and b can be coalesced via value.
	private boolean tryCoalesceCopyValue(Local a, Local b) {
		// System.out.println("Enter COPY");
		CongruenceClass conClassA = getCongruenceClass(a);
		CongruenceClass conClassB = getCongruenceClass(b);

		// System.out.println(" A: " + conClassA);
		// System.out.println(" B: " + conClassB);

		// System.out.println(" same=" + (conClassA == conClassB));
		if (conClassA == conClassB)
			return true;

		if (conClassA.size() == 1 && conClassB.size() == 1) {
			boolean r = checkInterfereSingle(conClassA, conClassB);
			// System.out.println(" single=" + r);
			return r;
		}

		boolean r2 = checkInterfere(conClassA, conClassB);
		// System.out.println(" both=" + r2);
		if (r2) {
			return false;
		}

		// merge congruence classes
		merge(conClassA, conClassB);
		// System.out.println("Exit COPY");
		return true;
	}

	private CongruenceClass getCongruenceClass(Local l) {
		if (congruenceClasses.containsKey(l)) {
			return congruenceClasses.get(l);
		}
		CongruenceClass cc = new CongruenceClass();
		cc.add(l);
		congruenceClasses.put(l, cc);
		return cc;
	}

	// Process the copy a = b. Returns true of a and b can be coalesced via sharing.
	private boolean tryCoalesceCopySharing(Local a, Local b) {
		// if (!DO_SHARE_COALESCE)
		// return false;
		CongruenceClass pccX = getCongruenceClass(a);
		CongruenceClass pccY = getCongruenceClass(b);
		for (Local c : values.get(a)) {
			if (c == b || c == a || !checkPreDomOrder(c, a) || !intersect(a, c))
				continue;
			CongruenceClass pccZ = getCongruenceClass(c);

			// If X = Z and X != Y, the copy is redundant.
			if (pccX == pccZ && pccX != pccY) {
				return true;
			}

			// If X, Y, and Z are all different and if a and c are coalescable via value then the copy is redundant
			// after a and b have been coalesced as c already has the correct value.
			if (pccY != pccX && pccY != pccZ && pccX != pccZ && tryCoalesceCopyValue(a, c)) {
				return true;
			}
		}
		return false;
	}

	// if they are in the same pcvs they will have the same index.
	private boolean checkPreDomOrder(Local x, Local y) {
		return defuse.defIndex.get(x) < defuse.defIndex.get(y);
	}

	private void remapLocals() {
		// ok NOW we remap to avoid that double remap issue
		for (Entry<Local, CongruenceClass> e : congruenceClasses.entrySet()) {
			remap.put(e.getKey(), e.getValue().first());
		}
	}

	// Flatten ccs so that each local in each cc is replaced with a new representative local.
	private void applyRemapping() {
		GenericBitSet<BasicBlock> processed = cfg.createBitSet();
		GenericBitSet<BasicBlock> processed2 = cfg.createBitSet();
		for (Local e : remap.keySet()) {
			for (BasicBlock used : defuse.uses.getNonNull(e)) {
				if (processed.contains(used))
					continue;
				processed.add(used);
				for (Stmt stmt : used) {
					for (Expr s : stmt.enumerateOnlyChildren()) {
						if (s.getOpcode() == Opcode.LOCAL_LOAD) {
							VarExpr v = (VarExpr) s;
							v.setLocal(remap.getOrDefault(v.getLocal(), v.getLocal()));
						}
					}
				}
			}
			BasicBlock b = defuse.defs.get(e);
			if (processed2.contains(b))
				continue;
			processed2.add(b);

			for (Iterator<Stmt> it = b.iterator(); it.hasNext();) {
				Stmt stmt = it.next();
				if (stmt instanceof ParallelCopyVarStmt) {
					ParallelCopyVarStmt copy = (ParallelCopyVarStmt) stmt;
					for (Iterator<CopyPair> it2 = copy.pairs.iterator(); it2.hasNext();) {
						CopyPair p = it2.next();
						p.source = remap.getOrDefault(p.source, p.source);
						p.targ = remap.getOrDefault(p.targ, p.targ);
						if (p.source == p.targ)
							it2.remove();
					}
					if (copy.pairs.isEmpty())
						it.remove();
				} else if (stmt instanceof CopyVarStmt) {
					AbstractCopyStmt copy = (AbstractCopyStmt) stmt;
					VarExpr v = copy.getVariable();
					v.setLocal(remap.getOrDefault(v.getLocal(), v.getLocal()));
					if (!copy.isSynthetic() && copy.getExpression().getOpcode() == Opcode.LOCAL_LOAD)
						if (((VarExpr) copy.getExpression()).getLocal() == v.getLocal())
							it.remove();
				} else if (stmt instanceof CopyPhiStmt) {
					throw new IllegalArgumentException("Phi copy still in block?");
				}
			}
		}
		for (Local e : remap.keySet()) {
			defuse.defs.remove(e);
			defuse.uses.remove(e);
		}
	}

	private boolean checkInterfereSingle(CongruenceClass red, CongruenceClass blue) {
		Local a = red.first();
		Local b = blue.first();
		// we want a > b in dom order (b is parent)
		if (checkPreDomOrder(a, b)) {
			Local c = a;
			a = b;
			b = c;
		}

		// System.out.println(" a,b=" + a + ", " + b);
		// if (!DO_VALUE_INTERFERENCE) {
		// boolean r1 = intersect(a, b);
		// System.out.println(" intersect=" + r1);
		// return r1;
		// }
		// System.out.println(" s valA,valB: " + values.getNonNull(a) + ", " +
		// values.getNonNull(b));

		if (intersect(a, b) && values.getNonNull(a) != values.getNonNull(b)) {
			return true;
		} else {
			equalAncIn.put(a, b);
			red.add(b);
			congruenceClasses.put(b, red);
			return false;
		}
	}

	// returns true if a's def dominates b's def
	private boolean dominates(Local a, Local b) {
		BasicBlock defA = defuse.defs.get(a);
		BasicBlock defB = defuse.defs.get(b);
		if (defA != defB) {
			// typical case (between blocks)
			return resolver.domc.getDominates(defA).contains(defB);
		} else {
			// special case (same basic block, rely on statement ordering within block)
			return checkPreDomOrder(a, b);
		}
	}

	private boolean checkInterfere(CongruenceClass red, CongruenceClass blue) {
		// System.out.println("Enter interfere: " + red + " vs " + blue);
		Stack<Local> dom = new Stack<>(); // dominator tree traversal stack
		Stack<Integer> domClasses = new Stack<>(); // 0=red, 1=blue
		int nr = 0, nb = 0; // from the paper
		Local ir = red.first(), ib = blue.first(); // iteration pointers
		Local lr = red.last(), lb = blue.last(); // end sentinels
		boolean redHasNext = true, blueHasNext = true;
		equalAncOut.put(ir, null); // these have no parents so we have to manually init them
		equalAncOut.put(ib, null);
		do {
			// System.out.println("\n ir, ib, lr, lb: " + ir + " " + ib + " " + lr + " " + lb);
			// System.out.println(" dom: " + dom);
			Local current;
			int currentClass;
			if (!blueHasNext || (redHasNext && checkPreDomOrder(ir, ib))) {
				// current = red[ir++] (Red case)
				current = ir;
				currentClass = 0;
				nr++;
				if (redHasNext = ir != lr)
					ir = red.higher(ir);
			} else {
				// current = blue[ib++] (Blue case)
				current = ib;
				currentClass = 1;
				nb++;
				if (blueHasNext = ib != lb)
					ib = blue.higher(ib);
			}

			while (!dom.isEmpty() && !dominates(dom.peek(), current)) {
				if (domClasses.peek() == 0)
					nr--;
				else if (domClasses.peek() == 1)
					nb--;
				else
					assert false;
				dom.pop();
				domClasses.pop();
			}
			if (!dom.isEmpty() && interference(current, dom.peek(), currentClass == domClasses.peek())) {
				return true;
			}
			dom.push(current);
			domClasses.push(currentClass);
		} while ((redHasNext && nb > 0) || (blueHasNext && nr > 0) || (redHasNext && blueHasNext));

		return false;
	}

	private boolean interference(Local a, Local b, boolean sameConClass) {
		// System.out.println("Check interference: " + a + " " + b + " " + sameConClass);
		equalAncOut.put(a, null);
		if (sameConClass) {
			b = equalAncOut.get(b);
		}
		if (b == null) {
			return false;
		}
		if (dominates(a, b))
			throw new IllegalArgumentException("b should dom a");

		Local tmp = b;
		while (tmp != null && !intersect(a, tmp)) {
			tmp = equalAncIn.get(tmp);
		}
		if (values.getNonNull(a) != values.getNonNull(b)) {
			// chain_intersect
			return tmp != null;
		} else {
			// update_equal_anc_out
			equalAncOut.put(a, tmp);
			return false;
		}
	}

	private boolean intersect(Local a, Local b) {
		if (a == b) {
			for (Entry<Local, CongruenceClass> e : congruenceClasses.entrySet()) {
				System.err.println(e.getKey() + " in " + e.getValue());
			}
			throw new IllegalArgumentException("me too thanks: " + a);
		}
		
		if (dominates(a, b))
			throw new IllegalArgumentException("b should dom a");

		BasicBlock defA = defuse.defs.get(a);
		// if it's liveOut it definitely intersects
		if (resolver.isLiveOut(defA, b))
			return true;
		// defA == defB or liveIn to intersect{
		if (!resolver.isLiveIn(defA, b) && defA != defuse.defs.get(b))
			return false;
		// ambiguous case. we need to check if use(dom) occurs after def(def), n that case it interferes. otherwise no
		int domUseIndex = defuse.lastUseIndex.getNonNull(b).getOrDefault(defA, -1);
		if (domUseIndex == -1) {
			return false;
		}
		int defDefIndex = defuse.defIndex.get(a);
		return domUseIndex > defDefIndex;
	}

	private void merge(CongruenceClass conClassA, CongruenceClass conClassB) {
		// System.out.println("Merge class " + conClassA + " <- " + conClassB);
		conClassA.addAll(conClassB);
		for (Local l : conClassB)
			congruenceClasses.put(l, conClassA);

		for (Local l : conClassA) {
			Local in = equalAncIn.get(l);
			Local out = equalAncOut.get(l);
			if (in != null && out != null)
				equalAncIn.put(l, checkPreDomOrder(in, out) ? out : in); // the MAXIMUM
			else if (in != null || out != null)
				equalAncIn.put(l, in != null ? in : out);
		}
	}

	private void sequentialize() {
		for (BasicBlock b : cfg.vertices())
			sequentialize(b);
	}

	private void sequentialize(BasicBlock b) {
		// TODO: just rebuild the instruction list
		LinkedHashMap<ParallelCopyVarStmt, Integer> p = new LinkedHashMap<>();
		for (int i = 0; i < b.size(); i++) {
			Stmt stmt = b.get(i);
			if (stmt instanceof ParallelCopyVarStmt)
				p.put((ParallelCopyVarStmt) stmt, i);
		}

		if (p.isEmpty())
			return;
		int indexOffset = 0;
		Local spill = locals.makeLatestVersion(p.entrySet().iterator().next().getKey().pairs.get(0).targ);
		for (Entry<ParallelCopyVarStmt, Integer> e : p.entrySet()) {
			ParallelCopyVarStmt pcvs = e.getKey();
			int index = e.getValue();
			if (pcvs.pairs.size() == 0)
				throw new IllegalArgumentException("pcvs is empty");
			else if (pcvs.pairs.size() == 1) { // constant sequentialize for trivial parallel copies
				CopyPair pair = pcvs.pairs.get(0);
				CopyVarStmt newCopy = new CopyVarStmt(new VarExpr(pair.targ, pair.type),
						new VarExpr(pair.source, pair.type));
				b.set(index + indexOffset, newCopy);
			} else {
				List<CopyVarStmt> sequentialized = pcvs.sequentialize(spill);
				b.remove(index + indexOffset--);
				for (CopyVarStmt cvs : sequentialized) { // warning: O(N^2) operation
					b.add(index + ++indexOffset, cvs);
				}
			}
		}
	}

	private class PhiRes {
		final Local target;
		final PhiExpr phi;
		final BasicBlock pred;
		final Local l;
		final Type type;

		PhiRes(Local target, PhiExpr phi, BasicBlock src, Local l, Type type) {
			this.target = target;
			this.phi = phi;
			pred = src;
			this.l = l;
			this.type = type;
		}

		@Override
		public String toString() {
			return String.format("targ=%s, p=%s, l=%s(type=%s)", target, pred, l, type);
		}
	}

	private class CopyPair {
		Local targ;
		Local source;
		Type type;

		CopyPair(Local dst, Local src, Type type) {
			targ = dst;
			source = src;
			this.type = type;
		}

		@Override
		public String toString() {
			return targ + " =P= " + source + " (" + type + ")";
		}
	}

	private class ParallelCopyVarStmt extends Stmt {
		public static final int PARALLEL_STORE = CLASS_RESERVED | CLASS_STORE | 0x1;
		final List<CopyPair> pairs;

		ParallelCopyVarStmt() {
			super(PARALLEL_STORE);
			pairs = new ArrayList<>();
		}

		ParallelCopyVarStmt(List<CopyPair> pairs) {
			super(PARALLEL_STORE);
			this.pairs = pairs;
		}

		@Override
		public void onChildUpdated(int ptr) {
		}

		@Override
		public void toString(TabbedStringWriter printer) {
			printer.print("PARALLEL(");

			for (Iterator<CopyPair> it = pairs.iterator(); it.hasNext();) {
				CopyPair p = it.next();
				printer.print(p.targ.toString());
				// printer.print("(" + p.type + ")");

				if (it.hasNext()) {
					printer.print(", ");
				}
			}

			printer.print(") = (");

			for (Iterator<CopyPair> it = pairs.iterator(); it.hasNext();) {
				CopyPair p = it.next();
				printer.print(p.source.toString());
				// printer.print("(" + p.type + ")");

				if (it.hasNext()) {
					printer.print(", ");
				}
			}

			printer.print(")");
		}

		private List<CopyVarStmt> sequentialize(Local spill) {
			Stack<Local> ready = new Stack<>();
			Stack<Local> to_do = new Stack<>();
			Map<Local, Local> loc = new HashMap<>();
			Map<Local, Local> pred = new HashMap<>();
			Map<Local, Type> types = new HashMap<>();
			pred.put(spill, null);

			for (CopyPair pair : pairs) { // initialization
				loc.put(pair.targ, null);
				loc.put(pair.source, null);
				types.put(pair.targ, pair.type);
				types.put(pair.source, pair.type);
			}

			for (CopyPair pair : pairs) {
				loc.put(pair.source, pair.source); // needed and not copied yet
				pred.put(pair.targ, pair.source); // unique predecessor
				to_do.push(pair.targ); // copy into b to be done
			}

			for (CopyPair pair : pairs) {
				if (!loc.containsKey(pair.targ))
					throw new IllegalStateException("this shouldn't happen");
				if (loc.get(pair.targ) == null) // b is not used and can be overwritten
					ready.push(pair.targ);
			}

			List<CopyVarStmt> result = new ArrayList<>();
			while (!to_do.isEmpty()) {
				while (!ready.isEmpty()) {
					Local b = ready.pop(); // pick a free location
					Local a = pred.get(b); // available in c
					Local c = loc.get(a);
					if ((!types.containsKey(b) && b != spill) || (!types.containsKey(c) && c != spill))
						throw new IllegalStateException("this shouldn't happen " + b + " " + c);

					VarExpr varB = new VarExpr(b, types.get(b)); // generate the copy b = c
					VarExpr varC = new VarExpr(c, types.get(b));
					result.add(new CopyVarStmt(varB, varC));

					loc.put(a, b);
					if (a == c && pred.get(a) != null) {
						if (!pred.containsKey(a))
							throw new IllegalStateException("this shouldn't happen");
						ready.push(a); // just copied, can be overwritten
					}
				}

				Local b = to_do.pop();
				if (b != loc.get(pred.get(b))) {
					if (!types.containsKey(b))
						throw new IllegalStateException("this shouldn't happen");
					VarExpr varN = new VarExpr(spill, types.get(b)); // generate the copy n = b
					VarExpr varB = new VarExpr(b, types.get(b));
					result.add(new CopyVarStmt(varN, varB));
					loc.put(b, spill);
					ready.push(b);
				}
			}

			return result;
		}

		@Override
		public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
			throw new UnsupportedOperationException("Synthetic");
		}

		@Override
		public boolean canChangeFlow() {
			return false;
		}

		@Override
		public ParallelCopyVarStmt copy() {
			return new ParallelCopyVarStmt(new ArrayList<>(pairs));
		}

		@Override
		public boolean equivalent(CodeUnit s) {
			return s instanceof ParallelCopyVarStmt && ((ParallelCopyVarStmt) s).pairs.equals(pairs);
		}
	}

	private class CongruenceClass extends TreeSet<Local> {
		private static final long serialVersionUID = -4472334406997712498L;

		protected CongruenceClass() {
			super(new Comparator<Local>() {
				@Override
				public int compare(Local o1, Local o2) {
					if (o1 == o2)
						return 0;
					return ((BoissinotDestructor.this.defuse.defIndex.get(o1) - BoissinotDestructor.
							this.defuse.defIndex.get(o2))) >> 31 | 1;
				}
			});
		}
	}

	// Used for dumping the dominator graph
	// private static <N extends FastGraphVertex> void dumpGraph(FastDirectedGraph<N, FastGraphEdge<N>> g, String name) {
    //     try {
	// 		Exporter.fromGraph(GraphUtils.makeDotSkeleton(g)).export(new File("cfg testing", name + ".png"));
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 	}
    // }
}
