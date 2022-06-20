package org.mapleir.deob.passes;

import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.FlowEdges;
import org.mapleir.flowgraph.edges.ImmediateEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.ssaopt.ConstraintUtil;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.mapleir.stdlib.collections.graph.algorithms.SimpleDfs;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class DeadCodeEliminationPass implements IPass {
	public int deadBlocks = 0;
	public int immediateJumps = 0;
	public int deadLocals = 0;
	
	public void process(ControlFlowGraph cfg) {
		LocalsPool lp = cfg.getLocals();
		
		boolean c;
		
		do {
			c = false;
			
			SimpleDfs<BasicBlock> dfs = new SimpleDfs<>(cfg, cfg.getEntries().iterator().next(), SimpleDfs.PRE);
			
			List<BasicBlock> pre = dfs.getPreOrder();
			for(BasicBlock b : new HashSet<>(cfg.vertices())) {
				if(!pre.contains(b)) {
//					System.out.println("proc1: " + b);
					for(FlowEdge<BasicBlock> fe : new HashSet<>(cfg.getEdges(b))) {
						cfg.exciseEdge(fe);
					}
//					System.out.println("removed: ");
					for(Stmt stmt : b) {
//						System.out.println(" " + (b.indexOf(stmt)) + ". " + stmt);
						if(stmt instanceof AbstractCopyStmt) {
							AbstractCopyStmt copy = (AbstractCopyStmt) stmt;
							lp.defs.remove(copy.getVariable().getLocal());
//							System.out.println("  kill1 " + copy.getVariable().getLocal());
						}
						
						for(Expr e : stmt.enumerateOnlyChildren()) {
							if(e.getOpcode() == Opcode.LOCAL_LOAD) {
								VarExpr v = (VarExpr) e;
								lp.uses.get(v.getLocal()).remove(v);
//								System.out.println("  kill2 " + v.getLocal());
							}
						}
					}
					cfg.removeVertex(b);
					
					deadBlocks++;
					c = true;
				} else {
//					System.out.println("proc2: " + b);
					UnconditionalJumpEdge<BasicBlock> uncond = null;
					
					for(FlowEdge<BasicBlock> fe : cfg.getEdges(b)) {
						if(fe.getType() == FlowEdges.UNCOND) {
							uncond = (UnconditionalJumpEdge<BasicBlock>) fe;
						}
					}
					
					if(uncond != null) {
						BasicBlock dst = uncond.dst();
						
						List<BasicBlock> verts = new ArrayList<>(cfg.vertices());
						
						if(verts.indexOf(b) + 1 == verts.indexOf(dst)) {
							ImmediateEdge<BasicBlock> im = new ImmediateEdge<>(b, dst);
							cfg.exciseEdge(uncond);
							cfg.addEdge(im);
							
							Stmt stmt = b.remove(b.size() - 1);
							
							if(stmt.getOpcode() != Opcode.UNCOND_JUMP) {
								throw new IllegalStateException(b + " : " + stmt);
							}
							
							immediateJumps++;
							c = true;
						}
					}
					
					// if(cfg.getMethod().toString().equals("cf.k(IIIIII)V")) {}
					
					Iterator<Stmt> it = b.iterator();
					while(it.hasNext()) {
						Stmt stmt = it.next();
						
						if(stmt.getOpcode() == Opcode.LOCAL_STORE) {
							AbstractCopyStmt copy = (AbstractCopyStmt) stmt;
							
							if(copy.isSynthetic()) {
								continue;
							}
							
							Local l = copy.getVariable().getLocal();
							LocalsPool pool = cfg.getLocals();

							if (!(l instanceof VersionedLocal) || pool == null || pool.uses.get(l) == null)
								continue;
							// System.out.println("copy: "+ copy);
							if(!ConstraintUtil.isUncopyable(copy.getExpression()) && pool.uses.get(l).size() == 0) {
								for(Expr e : copy.getExpression().enumerateWithSelf()) {
									if(e.getOpcode() == Opcode.LOCAL_LOAD) {
										VarExpr v = (VarExpr) e;
										
										Local l2 = v.getLocal();
										pool.uses.remove(l2);
									}
								}
								
								pool.uses.remove(l);
								pool.defs.remove(l);
								it.remove();
								
								deadLocals++;
								c = true;
							}
						} else if (stmt.getOpcode() == Opcode.NOP) {
							it.remove();
							c = true;
						}
					}
				}
			}
			
			// for now
		} while (c);
	}

	@Override
	public PassResult accept(PassContext pcxt) {
		AnalysisContext cxt = pcxt.getAnalysis();
		deadBlocks = 0;
		immediateJumps = 0;
		deadLocals = 0;
		
		for (ClassNode cn : cxt.getApplication().iterate()) {
			for (MethodNode m : cn.getMethods()) {
				ControlFlowGraph cfg = cxt.getIRCache().getFor(m);

				/* dead blocks */

 				process(cfg);
			}
		}

		System.out.printf("  removed %d dead blocks.%n", deadBlocks);
		System.out.printf("  converted %d immediate jumps.%n", immediateJumps);
		System.out.printf("  eliminated %d dead locals.%n", deadLocals);
		
		return PassResult.with(pcxt, this).finished(deadBlocks + immediateJumps).make();
	}
}
