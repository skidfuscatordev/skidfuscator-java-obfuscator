package org.mapleir.ir.cfg.builder;

import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.FlowEdges;
import org.mapleir.ir.algorithms.Liveness;
import org.mapleir.ir.algorithms.SSABlockLivenessAnalyser;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.builder.ssaopt.Constraint;
import org.mapleir.ir.cfg.builder.ssaopt.ConstraintUtil;
import org.mapleir.ir.cfg.builder.ssaopt.LatestValue;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.code.stmt.copy.CopyPhiStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.BasicLocal;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.mapleir.ir.utils.CFGUtils;
import org.mapleir.stdlib.collections.graph.algorithms.LT79Dom;
import org.mapleir.stdlib.collections.graph.algorithms.SimpleDfs;
import org.mapleir.stdlib.collections.list.IndexedList;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.Map.Entry;

/**
 * The compiler pass responsible for converting a non-SSA flow graph into a SSA-form flow graph.
 * Also does constant and copy propagation and expression inlining if enabled.
 * The input flowgraph *must* be naturalised; see {@link NaturalisationPass} for more details.
 *
 * No one really understands how this class works anymore. It works extremely well, but I swear
 * debugging this is next to impossible. Good luck.
 */
public class SSAGenPass extends ControlFlowGraphBuilder.BuilderPass {
	private final BasicLocal svar0;
	private final Map<VersionedLocal, Type> types;
	private final Map<Local, Integer> counters;
	private final Map<Local, Stack<Integer>> stacks;
	private final List<BasicBlock> order;
	// TODO: use arrays.
	private final Map<BasicBlock, Integer> insertion;
	private final Map<BasicBlock, Integer> process;
	private final Map<BasicBlock, Integer> preorder;
	private final Set<BasicBlock> handlers;
	
	private final Map<VersionedLocal, LatestValue> latest;
	private final Set<VersionedLocal> deferred;
	private final NullPermeableHashMap<VersionedLocal, Set<VersionedLocal>> shadowed;

	private LocalsPool pool;
	private LT79Dom<BasicBlock, FlowEdge<BasicBlock>> doms;
	
	private Liveness<BasicBlock> liveness;

	private final boolean optimise;

	public SSAGenPass(ControlFlowGraphBuilder builder) {
		this(builder, true);
	}

	public SSAGenPass(ControlFlowGraphBuilder builder, boolean optimise) {
		super(builder);
		this.optimise = optimise;

		svar0 = builder.graph.getLocals().newLocal(0, true);
		
		types = new HashMap<>();
		
		counters = new HashMap<>();
		stacks = new HashMap<>();
		order = new ArrayList<>();
		
		insertion = new HashMap<>();
		process = new HashMap<>();
		
		preorder = new HashMap<>();
		handlers = new HashSet<>();
		
		latest = new HashMap<>();
		deferred = new HashSet<>();
		shadowed = createCongMap();
	}
	
	private static NullPermeableHashMap<VersionedLocal, Set<VersionedLocal>> createCongMap() {
		return new NullPermeableHashMap<>(k -> {
			Set<VersionedLocal> set = new HashSet<>();
			set.add(k);
			return set;
		});
	}
	
	private void splitRanges() {
		// produce cleaner cfg
		List<BasicBlock> order = new ArrayList<>(builder.graph.verticesInOrder());
		NullPermeableHashMap<BasicBlock, Set<Local>> splits = new NullPermeableHashMap<>(HashSet::new);
		
		for(ExceptionRange<BasicBlock> er : builder.graph.getRanges()) {
			BasicBlock h = er.getHandler();
			handlers.add(h);
			
			Set<Local> ls = new HashSet<>(liveness.in(h));
			for(BasicBlock b : er.getNodes()) {
				splits.getNonNull(b).addAll(ls);
			}
		}
		
		for(Entry<BasicBlock, Set<Local>> e : splits.entrySet()) {
			BasicBlock b = e.getKey();
			Set<Local> ls = e.getValue();
			
			ArrayList<Stmt> stmtsCopy = new ArrayList<>(b);
			int i = 0;
			boolean checkSplit = false;
			for (int i1 = 0; i1 < stmtsCopy.size(); i1++) {
				Stmt stmt = stmtsCopy.get(i1);
				if (b.size() == i)
					throw new IllegalStateException("s");
				
				if (checkSplit && stmt.getOpcode() == Opcode.LOCAL_STORE) {
					CopyVarStmt copy = (CopyVarStmt) stmt;
					VarExpr v = copy.getVariable();
					if (ls.contains(v.getLocal())) {
						BasicBlock n = splitBlock(b, i);
						order.add(order.indexOf(b), n);
						i = 0;
						checkSplit = false;
					}
				} else {
					// do not split if we have only seen simple or synthetic copies (catch copy is synthetic)
					if (stmt instanceof CopyVarStmt) {
						CopyVarStmt copy = (CopyVarStmt) stmt;
						int opc = copy.getExpression().getOpcode();
						if (!copy.isSynthetic() && opc != Opcode.LOCAL_LOAD && opc != Opcode.CATCH) {
							checkSplit = true;
						}
					} else {
						checkSplit = true;
					}
				}
				i++;
			}
		}
		
		builder.graph.relabel(order);
		List<BasicBlock> dfsPreorder = SimpleDfs.preorder(builder.graph, builder.head);
		assert(dfsPreorder.size() == order.size());
		
		int po = 0;
		for(BasicBlock b : dfsPreorder) {
			insertion.put(b, 0);
			process.put(b, 0);
			preorder.put(b, po++);
		}
	}

	private BasicBlock splitBlock(BasicBlock b, int to) {
		// System.out.println("Splitting block " + b.getDisplayName() + " upto " + to);
		BasicBlock newBlock = CFGUtils.splitBlock(builder.factory, builder.graph, b, to, true);

		// update assigns
		Set<Local> assignedLocals = new HashSet<>();
		for (Stmt stmt : b)
			if (stmt.getOpcode() == Opcode.LOCAL_STORE)
				assignedLocals.add(((CopyVarStmt) stmt).getVariable().getLocal());
		for (Stmt stmt : newBlock) {
			if (stmt.getOpcode() == Opcode.LOCAL_STORE) {
				Local copyLocal = ((CopyVarStmt) stmt).getVariable().getLocal();
				Set<BasicBlock> set = builder.assigns.get(copyLocal);
				set.add(newBlock);
				if (!assignedLocals.contains(copyLocal))
					set.remove(b);
			}
		}

		return newBlock;
	}
	
	private void insertPhis() {
		int i = 0;
		for(Local l : builder.locals) {
			i++;

			assert builder.assigns.get(l) != null : "Failed to find assigns for " + l + "\n\n" + builder.graph.toString();
			
			LinkedList<BasicBlock> queue = new LinkedList<>();
			for(BasicBlock b : builder.assigns.get(l)) {
				process.put(b, i);
				queue.add(b);
			}
			while(!queue.isEmpty()) {
				insertPhis(queue.poll(), l, i, queue);
			}
		}
	}
	
	private void insertPhis(BasicBlock b, Local l, int i, LinkedList<BasicBlock> queue) {
		if(b == null || b == builder.head) {
			return; // exit
		}

		Local newl = builder.graph.getLocals().get(l.getIndex(), 0, l.isStack());
		
		for(BasicBlock x : doms.getIteratedDominanceFrontier(b)) {
			if(insertion.get(x) < i) {
				// pruned SSA
				if(liveness.in(x).contains(l)) {
					/* Scenarios: (assuming live in)
					 *   multiple lvar into any block      -> add phi
					 *   
					 *   svar0 into handler from exception 
					 *    and non exception edge and       -> add ephi(psi)
					 */
					
					if((l == svar0) && handlers.contains(x) /* == faster than contains. */) {
						/* Note: this is quite subtle. Since there is a
						 * copy, (svar0 = catch()) at the start of each
						 * handler block, technically any natural flowing
						 * svar0 definition is killed upon entry to the
						 * block, so it is not considered live. One way to
						 * check if the variable is live-in, therefore, is
						 * by checking whether svar0 is live-out of the
						 * catch() definition. We handle it here, since
						 * the previous liveness check which is used for
						 * pruned SSA will fail in this case. */
						/* Ok fuck that that, it's considered live-in
						 * even if there is a catch()::
						 *  #see SSaBlockLivenessAnalyser.precomputeBlock*/
						boolean naturalFlow = false;
						for(FlowEdge<BasicBlock> e : builder.graph.getReverseEdges(x)) {
							if(e.getType() != FlowEdges.TRYCATCH) {
								naturalFlow = true;
								break;
							}
						}
						if(naturalFlow) {
							CopyVarStmt catcher = null;
							
							for(Stmt stmt : x) {
								if(stmt.getOpcode() == Opcode.LOCAL_STORE) {
									CopyVarStmt copy = (CopyVarStmt) stmt;
									Expr e = copy.getExpression();
									if(e.getOpcode() == Opcode.CATCH) {
										catcher = copy;
										break;
									}
								}
							}
							
							if(catcher == null) {
								/* Handler but no catch copy?
								 * This can't happen since svar0 is
								 * the only reserved register for
								 * catch copies, and this block cannot
								 * be visited twice to insert a phi or
								 * psi(ephi) node. */
								throw new IllegalStateException(x.getDisplayName());
							}

							// this is no longer a problem as this is now handled in NaturalisationPass.
							throw new UnsupportedOperationException("Natural flow into a handler block in " + builder.method.toString() + ". Did you run NaturalisationPass?");
						}
					}
					
					if(builder.graph.getReverseEdges(x).size() > 1) {
						Map<BasicBlock, Expr> vls = new HashMap<>();
						for(FlowEdge<BasicBlock> fe : builder.graph.getReverseEdges(x)) {
							vls.put(fe.src(), builder.factory.var_expr()
									.local(newl)
									.type(null)
									.lifted(true)
									.build()
							);
						}
						PhiExpr phi = builder.factory.phi_expr()
								.args(vls)
								.build();
						CopyPhiStmt assign = builder.factory.copy_phi_stmt()
								.var(builder.factory.var_expr()
									.local(l)
									.type(null)
									.lifted(true)
									.build()
								)
								.phi(phi)
								.build();
						
						x.add(0, assign);
					}
				}
				
				insertion.put(x, i);
				if(process.get(x) < i) {
					process.put(x, i);
					queue.add(x);
				}
			}
		}
	}
	
	private void rename() {
		for(Local l : builder.locals) {
			counters.put(l, 0);
			stacks.put(l, new Stack<>());
		}

		List<BasicBlock> topoorder = new IndexedList<>(SimpleDfs.topoorder(builder.graph, builder.head));
		Set<BasicBlock> vis = new HashSet<>();
		search(builder.head, vis, topoorder);
		
		updatePhiArgTypes(vis);
	}
	
	private void updatePhiArgTypes(Set<BasicBlock> vis) {
		// update types for phi args
		for(BasicBlock b : order) {
			for(Stmt s : b) {
				if(s.getOpcode() != Opcode.PHI_STORE) {
					break;
				}
				
				CopyPhiStmt cps = (CopyPhiStmt) s;
				
				for(Entry<BasicBlock, Expr> e : cps.getExpression().getArguments().entrySet()) {
					BasicBlock src = e.getKey();
					if(vis.contains(src))
						continue;
					
					VarExpr v = (VarExpr) e.getValue();
					Local l = v.getLocal();
					// what if the def is never reached?
					AbstractCopyStmt def = pool.defs.get(l);
					v.setType(def.getType());
				}
			}
		}
	}
	
	private void search(BasicBlock b, Set<BasicBlock> vis, List<BasicBlock> order) {
		if(vis.contains(b)) {
			return;
		}
		vis.add(b);
		
		searchImpl(b);

		List<FlowEdge<BasicBlock>> succs = new ArrayList<>(builder.graph.getEdges(b));
		succs.sort(Comparator.comparing(o -> order.indexOf(o.dst())));
		
		for(FlowEdge<BasicBlock> succE : succs) {
			BasicBlock succ = succE.dst();
			fixPhiArgs(b, succ);
		}
		
		for(FlowEdge<BasicBlock> succE : succs) {
			BasicBlock succ = succE.dst();
			search(succ, vis, order);
		}
		
		unstackDefs(b);
		
		if(optimise || false) {
			optimisePhis(b);
		}
	}
	
	private void fixPhiArgs(BasicBlock b, BasicBlock succ) {
		for(Stmt stmt : succ) {
			if(stmt.getOpcode() == Opcode.PHI_STORE) {
				CopyPhiStmt copy = (CopyPhiStmt) stmt;
				PhiExpr phi = copy.getExpression();
				Expr e = phi.getArgument(b);
				
				if(e.getOpcode() == Opcode.LOCAL_LOAD) {
					VarExpr v = (VarExpr) e;
					
					translate(v, true, true);
					
					VersionedLocal ssaL = (VersionedLocal) v.getLocal();
					
					Type t = types.get(ssaL);
					copy.getVariable().setType(t);
					phi.setType(t);
				} else {
					throw new IllegalArgumentException(phi + ", " + e);
				}
			} else {
				/* No need to search the rest of the block
				 * after we have visited the phis as they
				 * precede all other statements.
				 */
				break;
			}
		}
	}
	
	private void unstackDefs(BasicBlock b) {
		for (Stmt s : b) {
			if (s.getOpcode() == Opcode.PHI_STORE || s.getOpcode() == Opcode.LOCAL_STORE) {
				AbstractCopyStmt cvs = (AbstractCopyStmt) s;
				Local l = cvs.getVariable().getLocal();
				l = builder.graph.getLocals().get(l.getIndex(), l.isStack());
				stacks.get(l).pop();
			}
		}
	}
	
	private void searchImpl(BasicBlock b) {
		Iterator<Stmt> it = b.iterator();
		
		while(it.hasNext()) {
			Stmt stmt = it.next();
			
			int opcode = stmt.getOpcode();
			
			if(opcode == Opcode.POP) {
				PopStmt pop = (PopStmt) stmt;
				if(!ConstraintUtil.isUncopyable(pop.getExpression())) {
					it.remove();
					continue;
				}
			}
			
			if(opcode == Opcode.PHI_STORE) {
				/* We can rename these any time as these
				 * are visited before all other statements
				 * in a block (since they are always
				 * the starting statements of a block, if
				 * that block contains phi statements).
				 */
				CopyPhiStmt copy = (CopyPhiStmt) stmt;
				generate(copy);
			} else {
				/* Translates locals into their latest SSA
				 * versioned locals.
				 * 
				 * Do this before a LOCAL_STORE (x = ...)
				 * so that the target local isn't defined
				 * before the use so that copies in the
				 * form x = x; do not get mangled into
				 * x0 = x0 after SSA renaming.
				 * 
				 * We rename phi args later as the source
				 * local can originate from exotic blocks.
				 */
				translate(stmt, true, false);
			}
			
			if(opcode == Opcode.LOCAL_STORE) {
				/* Generate the target local after
				 * renaming the source pool.uses. 
				 */
				CopyVarStmt copy = (CopyVarStmt) stmt;
				generate(copy);
			}
		}
	}
	
	private VersionedLocal generate(AbstractCopyStmt copy) {
		VarExpr v = copy.getVariable();
		Local oldLocal = v.getLocal();
		int index = oldLocal.getIndex();
		boolean isStack = oldLocal.isStack();
		
		LocalsPool handler = builder.graph.getLocals();
		Local l = handler.get(index, isStack);
		int subscript = counters.get(l);
		stacks.get(l).push(subscript);
		counters.put(l, subscript + 1);
		
		VersionedLocal ssaL = handler.get(index, subscript, isStack);
		
		if(optimise || true) {
			makeValue(copy, ssaL);
		}
		
		v.setLocal(ssaL);
		pool.defs.put(ssaL, copy);
		types.put(ssaL, copy.getExpression().getType());
		pool.uses.put(ssaL, new HashSet<>());
		
		return ssaL;
	}
	
	private void optimisePhis(BasicBlock b) {
		List<CopyPhiStmt> phis = new ArrayList<>();
		for(Stmt stmt : b) {
			if(stmt.getOpcode() == Opcode.PHI_STORE) {
				phis.add((CopyPhiStmt) stmt);
			} else {
				break;
			}
		}
		
		if(phis.size() > 1) {
			Set<Set<CopyPhiStmt>> ccs = findPhiClasses(phis);
			
			for(Set<CopyPhiStmt> cc : ccs) {
				CopyPhiStmt pref = chooseRealPhi(cc);
//				System.out.println("want to merge:");
//				for(CopyPhiStmt s : cc) {
//					System.out.println("  " + s);
//				}
//				System.out.println(" keeping " + pref);
				reduceClass(cc, pref);
			}
		}
	}
	
	private void reduceClass(Set<CopyPhiStmt> cc, CopyPhiStmt preferred) {
		Set<CopyPhiStmt> useless = new HashSet<>(cc);
		useless.remove(preferred);

		VersionedLocal phiLocal = (VersionedLocal) preferred.getVariable().getLocal();

		/* all the *dead* phi class locals */
		Set<VersionedLocal> deadLocals = new HashSet<>();
		
		for (CopyPhiStmt def : useless) {
			VersionedLocal local = (VersionedLocal) def.getVariable().getLocal();
			deadLocals.add(local);

			def.getBlock().getGraph().exciseStmt(def);
//			System.out.println(" killing " + def);
		}
		
		for(VersionedLocal vl : deadLocals) {
			Set<VarExpr> deadVExprs = pool.uses.get(vl);
			
			for(VarExpr v : deadVExprs) {
				/* v.getLocal() == vl, i.e. dead phi local.
				 * Replace each dead var with the real one. */
				
				v.setLocal(phiLocal);
				pool.uses.get(phiLocal).add(v);
			}
			
			/* all dead. */
			/*pool.uses.get(vl)*/deadVExprs.clear();
			pool.defs.remove(vl);
		}
	}
	
	private CopyPhiStmt chooseRealPhi(Set<CopyPhiStmt> cc) {
		for (CopyPhiStmt cps : cc) {
			if (!cps.getVariable().getLocal().isStack()) {
				return cps;
			}
		}
		return cc.iterator().next();
	}
	
	private Set<Set<CopyPhiStmt>> findPhiClasses(Collection<CopyPhiStmt> phis) {
		NullPermeableHashMap<CopyPhiStmt, Set<CopyPhiStmt>> equiv = new NullPermeableHashMap<>(HashSet::new);
		
		for(CopyPhiStmt cps : phis) {
			if(equiv.containsKey(cps)) {
				continue;
			}
			
			PhiExpr phi = cps.getExpression();
			otherPhiFor: for(CopyPhiStmt cps2 : phis) {
				if(cps == cps2 || equiv.containsKey(cps2)) {
					continue;
				}
				
				PhiExpr phi2 = cps2.getExpression();
				
				if(!phi.getSources().equals(phi2.getSources())) {
					continue;
				}
				
				for(BasicBlock src : phi.getSources()) {
					Expr e1 = phi.getArgument(src);
					Expr e2 = phi2.getArgument(src);
					
					if(!isSameValue(e1, e2)) {
						continue otherPhiFor;
					}
				}
				
				/* phi equiv phi2; merge. */
				Set<CopyPhiStmt> cca = equiv.getNonNull(cps);
				Set<CopyPhiStmt> ccb = equiv.getNonNull(cps2);
				
				cca.add(cps2);
				ccb.add(cps);
				
				cca.addAll(ccb);
				
				for (CopyPhiStmt s : cca) {
					equiv.put(s, cca);
				}
			}
		}
		
		/* get rid of duplicates */
		return new HashSet<>(equiv.values());
	}
	
	private boolean shouldCoalesce(int opcode) {
		switch (opcode) {
			case Opcode.INVOKE:
			case Opcode.INIT_OBJ:
			case Opcode.ALLOC_OBJ:
			case Opcode.NEW_ARRAY:
			case Opcode.CATCH:
			case Opcode.EPHI:
			case Opcode.PHI:
			case Opcode.FIELD_LOAD:
				return false;
		};
		return true;
	}
	
	private Expr getValue(Expr e) {
		if(e.getOpcode() == Opcode.LOCAL_LOAD) {
			VarExpr v = (VarExpr) e;
			
			AbstractCopyStmt def = pool.defs.get(v.getLocal());
			Expr val = def.getExpression();
			
			if(!def.isSynthetic() && def.getOpcode() != Opcode.PHI_STORE) {
				return getValue(val);
			}
		}
		
		return e;
	}
	
	private boolean isSameValue(Expr e1, Expr e2) {
		Expr val1 = getValue(e1);
		Expr val2 = getValue(e2);
		
		if(!shouldCoalesce(val1.getOpcode()) || !shouldCoalesce(val2.getOpcode())) {
			return false;
		}
		
		return val1.equivalent(val2);
	}
	
	private void makeValue(AbstractCopyStmt copy, VersionedLocal ssaL) {
		/* Attempts to find the 'value' of a local.
		 * The value can be the following types:
		 *   param - assigned by caller method
		 *   phi - set by a phi node
		 *   const - compiletime constant
		 *   var - runtime computed
		 *   
		 * when a copy x = y, is visited,
		 * if y is a var, x inherits
		 * the value and value type.
		 * */
		Expr e = copy.getExpression();
		int opcode = e.getOpcode();
		if(opcode == Opcode.LOCAL_LOAD) {
			if(copy.isSynthetic()) {
				/* equals itself (pure value).*/
				LatestValue value = new LatestValue(builder.graph, LatestValue.PARAM, ssaL, null /*null or ssaL*/);
				latest.put(ssaL, value);
			} else {
				/* i.e. x = y, where x and y are both variables.
				 * 
				 * It is expected that the local uses of the copy 
				 * (rhs) are visited before the target is.
				 */
				VarExpr rhs = (VarExpr) e;
				VersionedLocal rhsL = (VersionedLocal) rhs.getLocal();
				/* the rhsL must have been visited already
				 * and the lhsL must not have been.*/
				if(!latest.containsKey(ssaL)) {
					if(latest.containsKey(rhsL)) {
						LatestValue anc = latest.get(rhsL);
						// TODO: maybe advance the src local if we
						// can validate an expr propagation to the
						// new copy dst.
						LatestValue value = new LatestValue(builder.graph, anc.getType(), rhsL, anc.getSuggestedValue(), anc.getSource());
						value.importConstraints(anc);
						latest.put(ssaL, value);
					} else {
						throw new IllegalStateException("Non anc parent: " + ssaL + " = " + rhsL + " (def: " + pool.defs.get(rhsL) + ")");
					}
				} else {
					throw new IllegalStateException("Revisit def " + ssaL + " ( = " + rhsL + ")");
				}	
			}
		} else {
			LatestValue value;
			if(opcode == Opcode.CONST_LOAD) {
				ConstantExpr ce = (ConstantExpr) e;
				value = new LatestValue(builder.graph, LatestValue.CONST, ce, null);
			} else if((opcode & Opcode.CLASS_PHI) == Opcode.CLASS_PHI){
				value = new LatestValue(builder.graph, LatestValue.PHI, ssaL, null);
			} else {
				if(e.getOpcode() == Opcode.LOCAL_LOAD) {
					throw new RuntimeException(copy + "    " + e);
				}
				value = new LatestValue(builder.graph, LatestValue.VAR, e, ssaL);
				value.makeConstraints(e);
			}
			latest.put(ssaL, value);
		}
		
//		System.out.println("made val " + ssaL + " -> " + latest.get(ssaL));
	}
	
	private void collectUses(Expr e) {
		for(Expr c : e.enumerateWithSelf()) {
			if(c.getOpcode() == Opcode.LOCAL_LOAD) {
				VarExpr ve = (VarExpr) c;
				pool.uses.get(ve.getLocal()).add(ve);
			}
		}
	}
	
	private void translate(CodeUnit u, boolean resolve, boolean isPhi) {
		/* At the point in the lifetime of
		 * the IR, we can only have var loads
		 * as child expressions of a phi.*/
		if(u.getOpcode() == Opcode.LOCAL_LOAD) {
			translateStmt((VarExpr) u, resolve, isPhi);
		} else if(!isPhi) {
			for(Expr c : u.getChildren()) {
				translate(c, resolve, false);
			}
		}
	}
	
	private boolean shouldPropagate(VersionedLocal ssaL, VersionedLocal vl) {
		return (ssaL.isStack() && !vl.isStack()) || (ssaL.isStack() == vl.isStack());
	}
	
	private void merge(VersionedLocal vla, VersionedLocal vlb) {
		Set<VersionedLocal> cca = shadowed.getNonNull(vla);
		Set<VersionedLocal> ccb = shadowed.getNonNull(vlb);
		
		cca.add(vlb);
		ccb.add(vla);
		
		cca.addAll(ccb);
		
		for (VersionedLocal l : cca) {
			shadowed.put(l, cca);
		}
	}
	
	private boolean canTransferHandlers(BasicBlock db, BasicBlock ub) {
		List<ExceptionRange<BasicBlock>> dr = db.cfg.getProtectingRanges(db);
		List<ExceptionRange<BasicBlock>> ur = ub.cfg.getProtectingRanges(ub);
		
		int drs = dr.size(), urs = ur.size();
		
		boolean transferable = false;
		
		if(drs > 0) {
			if(urs == 0) {
				// we can clone the range information.
//				for(ExceptionRange<BasicBlock> e : dr) {
//					e.addVertex(ub);
//					builder.graph.addEdge(ub, new TryCatchEdge<>(ub, e));
//				}
//				
//				transferable = true;
			} else {
				dr.removeAll(ur);
				
				if(dr.size() == 0) {
					transferable = true;
				}
			}
		} else if(urs == 0) {
			transferable = true;
		}
		
		return transferable;
	}
	
	private void translateStmt(VarExpr var, boolean resolve, boolean isPhi) {
		/* Here we only remap local variable loads
		 * on the right hand side of a statement or
		 * expression. This means that if we are able
		 * to simply replace a local load which has
		 * a constant or deferred local value.
		 * 
		 * However, if the value of the local is
		 * a complex expression we need to check that
		 * we can propagate it before we do.
		 * 
		 * Since we will only replace a single
		 * local load in the original expression,
		 * only 1 variable is killed. However, there
		 * may be local load expressions in the
		 * propagated expression. To account for this,
		 * these local loads must be counted as new
		 * uses (except for when an expression is
		 * moved instead of copied to a use site).*/
		
		Local l = var.getLocal();
		VersionedLocal ssaL;
		
		if(resolve) {
			ssaL = latest(l.getIndex(), l.isStack());
		} else {
			ssaL = (VersionedLocal) l;
		}
		
		pool.uses.get(ssaL).add(var);
		
		VersionedLocal newL = ssaL;

		boolean exists = true;

		if(optimise) {
			if(latest.containsKey(ssaL)) {
				/* Try to propagate a simple copy local
				 * to its use site. It is possible that
				 * a non simple copy (including phis)
				 * will not have a mapping. In this case
				 * they will not have an updated target.*/
				LatestValue value = latest.get(ssaL);
				boolean unpredictable = value.getType() == LatestValue.PARAM || value.getType() == LatestValue.PHI;
				
				if(unpredictable && ssaL != value.getSuggestedValue()) {
					VersionedLocal vl = (VersionedLocal) value.getSuggestedValue();
					if(shouldPropagate(ssaL, vl)) {
						newL = vl;
					}
				} else if(!isPhi && !unpredictable) {
					Expr e = null;
					
					AbstractCopyStmt def = pool.defs.get(ssaL);
					Expr rval = (Expr) value.getSuggestedValue();
					if(ConstraintUtil.isUncopyable(rval)) {
						/* A variable might have a value
						 * that is uncopyable such as an
						 * invoke or allocation call.
						 * 
						 * There are two ways this may happen:
						 *   x = call();
						 *  or
						 *   x = call();
						 *   y = x;
						 *   
						 * we defer optimising the first
						 * case till the end.
						 * 
						 * in the second case, we can
						 * propagate the source var (x)
						 * in place of the target (y). */
						newL = tryDefer(value, ssaL);
					} else {
						AbstractCopyStmt from = def;
						if(value.getSource() != null) {
							from = pool.defs.get(value.getSource());
						}
						
						if(!value.hasConstraints() || (canTransferHandlers(def.getBlock(), var.getBlock()) && value.canPropagate(from, var.getRootParent(), var, false))) {
							/*System.out.printf("d: %s%n", def);
							System.out.printf("f: %s%n", from);
							System.out.printf("u: %s%n", var.getRootParent());
							System.out.printf("l: %s%n", ssaL);
							System.out.printf("v: %s%n", value);
							System.out.printf("rv: %s%n", rval);
							System.out.printf("c: %b%n", value.hasConstraints());
							System.out.println();*/
							
							if(shouldCopy(rval)) {
								e = rval;
							} else {
								newL = tryDefer(value, ssaL);
							}
						} else if(value.getRealValue() instanceof VersionedLocal) {
							VersionedLocal realVal = (VersionedLocal) value.getRealValue();
							if(shouldPropagate(ssaL, realVal)) {
								newL = realVal;
							} else {
								// 1/21/2019: fix for propagation across multiple variables in a congruence class
								// (i.e. replacing two variables with one spill later in resolveShadowedLocals)
								merge(ssaL, realVal);
								// This code was incorrect
								// shadowed.getNonNull(ssaL).add(realVal);
								// shadowed.getNonNull(realVal).add(ssaL);
							}
						}
					}
					
					if(e != null) {
//						System.out.println("=====");
//						System.out.println("   ssaL: " + ssaL);
//						System.out.println("   bpar: " + var.getParent());
						CodeUnit parent = var.getParent();
						int idx = parent.indexOf(var);
						parent.writeAt(e = e.copy(), idx);
						
//						System.out.println("    def: " + def);
//						System.out.println("    idx: " + idx);
//						System.out.println("    val: " + value);
//						System.out.println("   apar: " + parent);
//						System.out.println("      e: " + e);

						/* Remove the use of the var before
						 * we translate the children of the 
						 * newly propagated expression.*/
						pool.uses.get(ssaL).remove(var);
//						System.out.println("   uses: " + pool.uses.get(ssaL));
						
						/* Account for the new pool.uses.*/
						collectUses(e);
						
						/* Finally see if we can reduce
						 * this statement further.*/
						translate(e, false, isPhi);
						
						exists = false;
					}
				} else {
					newL = ssaL;
				}
			} else {
				throw new IllegalStateException("No (self) ancestors: " + l + " -> " + ssaL);
			}
		}

		if(exists) {
			if(optimise || false) {
				/* If we removed the local load expression,
				 * check to see if we need to update the
				 * use-map.*/
				// System.out.println("replace: " + ssaL + " with " + newL);
				if(ssaL != newL) {
//					System.out.println(ssaL + "  -->  " + newL);
					pool.uses.get(ssaL).remove(var);
					pool.uses.get(newL).add(var);
				}
			}

			/* If the expression still exists, update
			 * or set both variable and type information.*/
			var.setLocal(newL);
			Type type = types.get(ssaL);
			if(type == null) {
				throw new IllegalStateException(var + ", " + ssaL + ", t=null");
			} else {
				var.setType(type);
			}
		}
	}
	
	private VersionedLocal tryDefer(LatestValue value, VersionedLocal ssaL) {
		VersionedLocal newL = ssaL;
		
		if(value.getRealValue() instanceof VersionedLocal) {
			VersionedLocal realVal = (VersionedLocal) value.getRealValue();
			if(shouldPropagate(ssaL, realVal)) {
				newL = realVal;
			} else {
				merge(ssaL, realVal);
			}
		}
		
		/* no change. */
		if(newL == ssaL) {
			deferred.add(newL);
		}
		
		return newL;
	}

	private boolean shouldCopy(Expr rval) {
		for(Expr e : rval.enumerateWithSelf()) {
			if(e.getOpcode() == Opcode.ARITHMETIC) {
				return false;
			}
		}
		return true;
	}

	private VersionedLocal latest(int index, boolean isStack) {
		LocalsPool handler = builder.graph.getLocals();
		Local l = handler.get(index, isStack);

		Stack<Integer> stack = stacks.get(l);
		if(stack == null || stack.isEmpty()) {
			System.err.println(builder.method.getOwner() + "#" + builder.method.getName());
			System.err.println(builder.graph);
			System.err.println(stacks);
			throw new NullPointerException(l.toString());
		}
		
		return handler.get(index, stack.peek()/*subscript*/, isStack);
	}
	
	private void aggregateInitialisers() {
		//System.out.println(builder.graph.toString());
		for(BasicBlock b : builder.graph.vertices()) {
			for(Stmt stmt : new ArrayList<>(b)) {
				if (stmt.getOpcode() == Opcode.POP) {
					PopStmt pop = (PopStmt) stmt;
					Expr expr = pop.getExpression();
					if (expr.getOpcode() == Opcode.INVOKE) {
						InvocationExpr invoke = (InvocationExpr) expr;
						if (invoke.getCallType() == InvocationExpr.CallType.SPECIAL && invoke.getName().equals("<init>")) {
							Expr inst = invoke.getPhysicalReceiver();

							if (inst.getOpcode() == Opcode.LOCAL_LOAD) {
								VarExpr var = (VarExpr) inst;
								VersionedLocal local = (VersionedLocal) var.getLocal();

								AbstractCopyStmt def = pool.defs.get(local);

								Expr rhs = def.getExpression();

								//System.out.println("def : " + stmt + " from " + rhs);

							} else if(inst.getOpcode() == Opcode.ALLOC_OBJ) {
								// replace pop(new Klass.<init>(args)) with pop(new Klass(args))
								// UninitialisedObjectExpr obj = (UninitialisedObjectExpr) inst;
								
								Expr[] args = invoke.getParameterExprs();
								// we want to reuse the exprs, so free it first.
								invoke.unlink();
								for(Expr e : args) {
									e.unlink();
								}
								
								Expr[] newArgs = Arrays.copyOf(args, args.length);
								InitialisedObjectExpr newExpr = new InitialisedObjectExpr(invoke.getOwner(), invoke.getDesc(), newArgs);
								// replace pop contents
								// no changes to defs or uses
								
								pop.setExpression(newExpr);
							} else {
								System.err.println(b);
								System.err.println("Stmt: " + stmt.getDisplayName() + ". " + stmt);
								System.err.println("Inst: " + inst);
								System.err.println(builder.graph);
								throw new RuntimeException("interesting1 " + inst.getClass());
							}
						}
					}
				}
			}
		}
	}
	
	private int pruneStatements() {
		int s = pool.uses.size();
		
		Iterator<Entry<VersionedLocal, Set<VarExpr>>> it = pool.uses.entrySet().iterator();
		while(it.hasNext()) {
			Entry<VersionedLocal, Set<VarExpr>> e = it.next();
			
			VersionedLocal vl = e.getKey();
			if(e.getValue().size() == 0) {
				AbstractCopyStmt def = pool.defs.get(vl);
				/* i.e. it has not been shadowed. */
				if(def != null && def.getBlock() != null && prune(def)) {
					if(vl != def.getVariable().getLocal()) {
						throw new RuntimeException(vl + ", " + def);
					}
					/* use pool remove */
					it.remove();
					pool.defs.remove(vl);
					
				}
			}
		}
		
		return s - pool.uses.size();
	}
	
	private boolean prune(AbstractCopyStmt def) {
		if(def.isSynthetic()) {
			return false;
		}
		
		Expr e = def.getExpression();
		
		if(canPrune(e)) {
			for(Expr s : e.enumerateWithSelf()) {
				if(s.getOpcode() == Opcode.LOCAL_LOAD) {
					VarExpr v = (VarExpr) s;
					VersionedLocal vl = (VersionedLocal) v.getLocal();
					pool.uses.get(vl).remove(v);
				}
			}
			
			def.getBlock().remove(def);
			return true;
		}
		
		return false;
	}
	
	private boolean canPrune(Expr e) {
		int op = e.getOpcode();
		
		if(op != Opcode.PHI && ConstraintUtil.isUncopyable(e)) {
			return false;
		}
		
		for(Expr s : e.getChildren()) {
			if(!canPrune(s)) {
				return false;
			}
		}
		
		return true;
	}
	
	private VersionedLocal findLowestSubscript(Set<VersionedLocal> set, int index) {
		VersionedLocal lowest = null;
		
		for(VersionedLocal vl : set) {
			if(vl.getIndex() == index) {
				if(lowest == null || vl.getSubscript() < lowest.getSubscript()) {
					lowest = vl;
				} else if(vl.getSubscript() == lowest.getSubscript()) {
					throw new IllegalStateException(String.format("idx:%d, sub:%d, low:%s", index, vl.getSubscript(), lowest));
				}
			}
		}
		
		return lowest;
	}
	
	private VersionedLocal findLowest(Set<VersionedLocal> set) {
		VersionedLocal lowest = null;
		
		for(VersionedLocal vl : set) {
			if(lowest == null || vl.getIndex() < lowest.getIndex()) {
				lowest = vl;
			} else if(vl.getIndex() == lowest.getIndex()) {
				return findLowestSubscript(set, vl.getIndex());
			}
		}
		
		return lowest;
	}
	
	private void removeSimpleCopy(AbstractCopyStmt copy) {
		VarExpr v = (VarExpr) copy.getExpression();
		VersionedLocal vl = (VersionedLocal) v.getLocal();
		copy.getBlock().remove(copy);
		
		pool.uses.get(vl).remove(v);
	}

	private void resolveShadowedLocals() {
		Set<VersionedLocal> visited = new HashSet<>();

		for(Entry<VersionedLocal, Set<VersionedLocal>> e : shadowed.entrySet()) {
			Set<VersionedLocal> shouldEqual = new HashSet(e.getValue());
			shouldEqual.add(e.getKey());
			for (VersionedLocal ccMember : e.getValue()) {
				Set<VersionedLocal> otherCc = new HashSet<>(shadowed.get(ccMember));
				otherCc.add(ccMember);
				if (!otherCc.equals(shouldEqual)) {
					System.err.println(shadowed);
					throw new IllegalStateException("Shadowed locals congruence classes are inconsistent!\n"
							+ "Faulty local = " + e.getKey() + " vs " + ccMember);
				}
			}
		}

		// System.out.println(shadowed);
		for(Entry<VersionedLocal, Set<VersionedLocal>> e : shadowed.entrySet()) {
			
			if(!visited.contains(e.getKey())) {
				Set<VersionedLocal> set = e.getValue();
				
				visited.addAll(set);
				
				Set<VersionedLocal> lvars = new HashSet<>();
				
				for(VersionedLocal l : set) {
					if(!l.isStack()) {
						lvars.add(l);
					}
				}
				
				/* find a suitable spill variable:
				 *  favour lvars
				 *  favour lowest version */
				VersionedLocal spill;
				
				if(lvars.isEmpty()) {
					/* all vars are svars. */
					spill = findLowest(set);
				} else if(lvars.size() == 1) {
					spill = lvars.iterator().next();
				} else {
					/* multiple lvars. // TODO: tweak? */
//					System.err.println(e.getKey() + "    " + lvars);
					spill = findLowest(lvars);
				}
				
//				System.out.println(set + "  spill; " + spill);
				
				/* now that we've chosen a spill var, we
				 * find the original copy, i.e. the one
				 * which has a runtime computed value
				 * as it's rhs. we then replace the
				 * target of that copy to the spill
				 * var and remove the definitions of the
				 * shadowed vars. we then rename all
				 * uses of the shadowed vars with the
				 * spill. */
				
				Set<AbstractCopyStmt> orig = new HashSet<>();
				
				Set<VarExpr> spillUses = pool.uses.get(spill);
				
//				System.out.println("spillUses: " + spillUses);
				
//				System.out.println(set);
				for(VersionedLocal vl : set) {
					AbstractCopyStmt copy = pool.defs.get(vl);
//					System.out.println(vl);
					Expr ex = copy.getExpression();
					if(vl != spill) {
						if(ex.getOpcode() != Opcode.LOCAL_LOAD) {
							orig.add(copy);
						} else {
//							System.out.println("del1: " + copy);
							removeSimpleCopy(copy);
						}
						
						/* transfer the uses of each shadowed
						 * var to the spill var, since we
						 * rename all of the shadowed vars. */
						Set<VarExpr> useSet = pool.uses.get(vl);
//						System.out.println("uses of " + vl + " ; " + useSet);
						for(VarExpr v : useSet) {
							v.setLocal(spill);
						}
						spillUses.addAll(useSet);
						
						useSet.clear();
						pool.uses.remove(vl);
					} else {
//						System.out.println("del2: " + copy);
						removeSimpleCopy(copy);
					}
					
					pool.defs.remove(vl);
				}
				
//				System.out.println(String.format("set:%s, spill:%s", set, spill));
//				System.out.println();
				
				if(orig.size() != 1) {
					throw new UnsupportedOperationException(String.format("set:%s, spill:%s, orig:%s", set, spill, orig));
				}
				
				AbstractCopyStmt copy = orig.iterator().next();
				copy.getVariable().setLocal(spill);
				pool.defs.put(spill, copy);
			}
		}
	}
	
	private int processDeferredTranslations() {
		int i = 0;
		
		Iterator<Entry<VersionedLocal, Set<VarExpr>>> it = pool.uses.entrySet().iterator();
		while(it.hasNext()) {
			Entry<VersionedLocal, Set<VarExpr>> e = it.next();
			VersionedLocal vl = e.getKey();
			
			if(deferred.contains(vl) || vl.isStack()) {
				Set<VarExpr> useSet = e.getValue();
				AbstractCopyStmt def = pool.defs.get(vl);
				if (def != null && useSet.size() == 1) {
					/* In this case, the only place that the value
					 * of this assignment will be used is at the use site.
					 * Since that value can not be spread until this one
					 * is, we can propagate it.*/
					if (def.getOpcode() != Opcode.PHI_STORE) {
						VarExpr use = useSet.iterator().next();
						
						LatestValue val = latest.get(vl);
//						System.out.println();
//						System.out.println();
//						System.out.println(def);
//						System.out.println(use);
						
						/* phi var*/
						Expr rhs = def.getExpression();
						
						if(use.getParent() != null) {
							if(canTransferHandlers(def.getBlock(), use.getBlock()) && val.canPropagate(def, use.getRootParent(), use, false)) {
								CodeUnit parent = use.getParent();
								if(rhs.getOpcode() == Opcode.CATCH) {
//									CodeUnit rp = use.getRootParent();
//									System.out.println("DENIED NIGGA");
//									System.out.println("replace " + vl + " with " + rhs);
//									System.out.println(" in " + parent);
//									System.out.println(" kill def: " + def);
//									System.out.println();
									deferred.remove(vl);
									continue;
									
									// check to see if we're moving it to the
									// first expression in the block, if we aren't
									// then deny, otherwise we can get rid of the local.
//									if(rp.getBlock().indexOf(rp) != 1 || rp.enumerateExecutionOrder().indexOf(use) != 0) {
//										
//									}
								}
								
								
								rhs.unlink();
								def.getBlock().remove(def);
								pool.defs.remove(vl);
								
								useSet.clear();
								parent.writeAt(rhs, parent.indexOf(use));
								
								i++;
								it.remove();
							}
						}
					}
				}
			}
		
		}
		
		return i;
	}
	
	private void makeLiveness() {
		SSABlockLivenessAnalyser liveness = new SSABlockLivenessAnalyser(builder.graph);
		liveness.compute();
		this.liveness = liveness;
	}
	
	@Override
	public void run() {
		pool = builder.graph.getLocals();
		

		order.addAll(builder.graph.verticesInOrder());
		order.remove(builder.head);
		order.add(0, builder.head);
		builder.graph.relabel(order);
		
		makeLiveness();
		splitRanges();
		makeLiveness();
		
		doms = new LT79Dom<>(builder.graph, builder.head);
		insertPhis();
		rename();

		resolveShadowedLocals();
		aggregateInitialisers();
		if(optimise) {


			int i;
			do {
				i = 0;
				i += processDeferredTranslations();
				i += pruneStatements();
			} while(i > 0);
		}
	}
}
