package org.mapleir.deob.passes.constparam;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.deob.interproc.IPAnalysis;
import org.mapleir.deob.interproc.IPAnalysisVisitor;
import org.mapleir.deob.intraproc.eval.ExpressionEvaluator;
import org.mapleir.deob.intraproc.eval.LocalValueResolver;
import org.mapleir.deob.intraproc.eval.impl.ReflectiveFunctorFactory;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.FlowEdges;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.stdlib.collections.taint.TaintableSet;
import org.objectweb.asm.Type;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public class ConstantExpressionEvaluatorPass implements IPass, Opcode {
	private ExpressionEvaluator evaluator;
	private int branchesEvaluated, exprsEvaluated;
	
	public ConstantExpressionEvaluatorPass() {
		evaluator = new ExpressionEvaluator(new ReflectiveFunctorFactory());
	}
	
	@Override
	public PassResult accept(PassContext pcxt) {
		AnalysisContext cxt = pcxt.getAnalysis();
		branchesEvaluated = 0;
		exprsEvaluated = 0;
		
		IPConstAnalysisVisitor vis = new IPConstAnalysisVisitor(cxt);
		IPAnalysis.create(cxt, vis);
		
		for(;;) {
			int prevExprsEval = exprsEvaluated;
			int prevBranchesEval = branchesEvaluated;
			
			for(ClassNode cn : cxt.getApplication().iterate()) {
				for(MethodNode m : cn.getMethods()) {
					processMethod(m, vis, cxt.getIRCache().getFor(m));
				}
			}
			
			if(prevExprsEval == exprsEvaluated && prevBranchesEval == branchesEvaluated) {
				break;
			}
		}
		
		System.out.printf("  evaluated %d constant expressions.%n", exprsEvaluated);
		System.out.printf("  eliminated %d constant branches.%n", branchesEvaluated);

		return PassResult.with(pcxt, this).finished(exprsEvaluated).make();
	}
	
	public void processMethod(MethodNode m, IPConstAnalysisVisitor vis, ControlFlowGraph cfg) {
		for(BasicBlock b : new HashSet<>(cfg.vertices())) {
			for(int i=0; i < b.size(); i++) {
				Stmt stmt = b.get(i);
				
				// simplify conditional branches.
				if(stmt.getOpcode() == COND_JUMP) {
					// todo: satisfiability analysis
					ConditionalJumpStmt cond = (ConditionalJumpStmt) stmt;
					Boolean result = evaluateConditional(vis, cfg, cond);
					if (result != null) {
						eliminateBranch(cfg, cond.getBlock(), cond, i, result);
						branchesEvaluated++;
					}
				}

				// evaluate arithmetic.
				for(CodeUnit cu : stmt.enumerateExecutionOrder()) {
					if(cu instanceof Expr) {
						Expr e = (Expr) cu;
						CodeUnit par = e.getParent();
						if(par != null) {
							Expr val = simplifyArithmetic(cfg.getLocals(), e);
							if (val != null) {
								exprsEvaluated++;
								cfg.writeAt(par, e, val);
							}
						}
					}
				}
			}
		}
	}
	
	private Expr simplifyArithmetic(LocalsPool pool, Expr e) {
		// no point evaluating constants
		if (e.getOpcode() == CONST_LOAD) {
			return null;
		}
		
		// try direct evaluation
		Expr val = evaluator.eval(pool, e);
		if(val != null && !val.equivalent(e)) {
			return val;
		}
		
		// try to simplify arithmetic
		if(e.getOpcode() == ARITHMETIC) {
			Expr e2 = evaluator.simplifyArithmetic(pool, (ArithmeticExpr) e);
			if (e2 != null) {
				return e2;
			}
		}
		
		return null;
	}
	
	private void eliminateBranch(ControlFlowGraph cfg, BasicBlock b, ConditionalJumpStmt cond, int insnIndex, boolean val) {
		if(val) { // always true, jump to true successor
			// remove immediate edge (it will never be taken)
			for(FlowEdge<BasicBlock> fe : new HashSet<>(cfg.getEdges(b))) {
				if(fe.getType() == FlowEdges.IMMEDIATE) {
					cfg.exciseEdge(fe);
				}
			}
			
			// create new jump and update cfg
			UnconditionalJumpEdge<BasicBlock> uje = new UnconditionalJumpEdge<>(b, cond.getTrueSuccessor());
			UnconditionalJumpStmt newJump = new UnconditionalJumpStmt(cond.getTrueSuccessor(), uje);
			b.set(insnIndex, newJump);
			cfg.addEdge(uje);
		} else { // always false, keep immediate (fallthrough) and remove the conditional branch.
			// remove statement amd uses in d/u map
			cfg.exciseStmt(cond);
			
			// remove conditional edge
			for(FlowEdge<BasicBlock> fe : new HashSet<>(cfg.getEdges(b))) {
				if(fe.getType() == FlowEdges.COND) {
					if(fe.dst() != cond.getTrueSuccessor()) {
						throw new IllegalStateException(fe + ", " + cond);
					}
					cfg.exciseEdge(fe);
				}
			}
		}
	}
	
	public Boolean evaluateConditional(IPConstAnalysisVisitor vis, ControlFlowGraph cfg, ConditionalJumpStmt cond) {
		Expr l = cond.getLeft();
		Expr r = cond.getRight();
		
		if (!TypeUtils.isPrimitive(l.getType()) || !TypeUtils.isPrimitive(r.getType())) {
			if(l instanceof ConstantExpr && r instanceof ConstantExpr && !TypeUtils.isPrimitive(l.getType()) && !TypeUtils.isPrimitive(r.getType())) {
				ConstantExpr left = (ConstantExpr) l;
				ConstantExpr right = (ConstantExpr) r;
				if (left.getConstant() == null && right.getConstant() == null) {
					return cond.getComparisonType() == ConditionalJumpStmt.ComparisonType.EQ;
				}
				if (cond.getComparisonType() == ConditionalJumpStmt.ComparisonType.EQ) {
					if ((left.getConstant() == null) != (right.getConstant() == null)) {
						return false;
					}
				}
				return null;
			}
			return null;
		}
		
		LocalValueResolver resolver = new SemiConstantLocalValueResolver(vis);
		
		TaintableSet<ConstantExpr> lSet = evaluator.evalPossibleValues(resolver, l);
		TaintableSet<ConstantExpr> rSet = evaluator.evalPossibleValues(resolver, r);

		/* can only evaluate branch if all vals are known. */
		if(!lSet.isTainted() && !rSet.isTainted()) {
			
			if(lSet.isEmpty() || rSet.isEmpty()) {
				
				/*System.err.println("oim interested m89");
				System.err.println("Empty:");
				System.err.println(cfg);
				System.err.println("inputs:");
				int k = 0;
				for(TaintableSet<ConstantExpr> s : vis.constParams.get(cfg)) {
					System.err.printf("@%d:: %s%n", k++, s);
				}
				System.err.println(l + " -> " + lSet);
				System.err.println(r + " -> " + rSet);
				System.err.println(cfg);*/


				return null;
			}
			
			Boolean result = evaluator.evaluatePrimitiveConditional(cond, lSet, rSet);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
	
	/**
	 * Provides values of locals based on constant parameter information from call trace (IPConstAnalysisVisitor)
	 */
	public static class SemiConstantLocalValueResolver implements LocalValueResolver {
		
		private final IPConstAnalysisVisitor vis;
		
		public SemiConstantLocalValueResolver(IPConstAnalysisVisitor vis) {
			this.vis = vis;
		}

		
		private boolean isVirtual(LocalsPool pool) {
			Local lvar0_0 = pool.get(0, 0, false);
			return pool.isImplicitRegister(lvar0_0);
		}
		
		@Override
		public TaintableSet<Expr> getValues(ControlFlowGraph cfg, Local l) {
			TaintableSet<Expr> set = new TaintableSet<>();
			
			LocalsPool pool = cfg.getLocals();
			AbstractCopyStmt copy = pool.defs.get(l);
			if(copy.isSynthetic()) {
				VarExpr vE = (VarExpr) copy.getExpression();
				if(vE.getLocal() != l) {
					throw new IllegalStateException(copy + " : " + l);
				}
				
				int paramNum = copy.getBlock().indexOf(copy);
				if(isVirtual(pool)) {
					/* for a virtual call, the implicit
					 * this object isn't considered a
					 * parameter, so the current computed
					 * paramNum is off by +1 (as it is
					 * including the lvar0_0 synth def). */
					paramNum -= 1;
				}
				
				set.union(vis.constParams.get(cfg).get(paramNum));
			} else {
				set.add(copy.getExpression());
			}
			
			return set;
		}
	}
	
	/**
	 * Stores information regarding constant parameters encountered during call tracing.
	 */
	public static class IPConstAnalysisVisitor implements IPAnalysisVisitor {
		final AnalysisContext cxt;
		final Map<ControlFlowGraph, List<TaintableSet<ConstantExpr>>> constParams = new HashMap<>();
		
		public IPConstAnalysisVisitor(AnalysisContext cxt) {
			this.cxt = cxt;
		}
		
		@Override
		public void postVisitMethod(IPAnalysis analysis, MethodNode m) {
			// Initialise possible value sets for parameters.
			int pCount = Type.getArgumentTypes(m.node.desc).length;
			
			ControlFlowGraph cfg = cxt.getIRCache().getFor(m);
			
			if(Modifier.isStatic(m.node.access)) {
				if(!constParams.containsKey(cfg)) {
					List<TaintableSet<ConstantExpr>> l = new ArrayList<>();
					constParams.put(cfg, l);
					
					for(int i=0; i < pCount; i++) {
						l.add(new TaintableSet<>());
					}
				}
			} else {
				for(MethodNode site : cxt.getInvocationResolver().resolveVirtualCalls(m, true)) {
					ControlFlowGraph siteCfg = cxt.getIRCache().getFor(site);
					
					if(!constParams.containsKey(siteCfg)) {
						List<TaintableSet<ConstantExpr>> l = new ArrayList<>();
						constParams.put(siteCfg, l);
						
						for(int i=0; i < pCount; i++) {
							l.add(new TaintableSet<>());
						}
					}
				}
			}
		}
		
		@Override
		public void postProcessedInvocation(IPAnalysis analysis, MethodNode caller, MethodNode callee, Invocation call) {
			// Update the parameter possible value sets to include the arguments of the processed call.
			Expr[] params = call.getParameterExprs();
			
			for(int i=0; i < params.length; i++) {
				Expr e = params[i];
				
				if(e.getOpcode() == Opcode.CONST_LOAD) {
					if(Modifier.isStatic(callee.node.access)) {
						constParams.get(cxt.getIRCache().getFor(callee)).get(i).add((ConstantExpr) e);
					} else {
						/* only chain callsites *can* have this input */
						for(MethodNode site : cxt.getInvocationResolver().resolveVirtualCalls(callee, true)) {
							constParams.get(cxt.getIRCache().getFor(site)).get(i).add((ConstantExpr) e);
						}
					}
				} else {
					/* callsites tainted */
					if(Modifier.isStatic(callee.node.access)) {
						constParams.get(cxt.getIRCache().getFor(callee)).get(i).taint();
					} else {
						/* only chain callsites *can* have this input */
						for(MethodNode site : cxt.getInvocationResolver().resolveVirtualCalls(callee, true)) {
							constParams.get(cxt.getIRCache().getFor(site)).get(i).taint();
						}
					}
				}
			}
		}
	}
}
