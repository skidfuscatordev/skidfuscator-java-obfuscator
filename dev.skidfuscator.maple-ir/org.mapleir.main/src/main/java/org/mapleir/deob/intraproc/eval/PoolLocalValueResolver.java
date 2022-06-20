package org.mapleir.deob.intraproc.eval;

import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.stdlib.collections.taint.TaintableSet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PoolLocalValueResolver implements LocalValueResolver {
	
	final LocalsPool pool;
	
	public PoolLocalValueResolver(LocalsPool pool) {
		this.pool = pool;
	}
	
	private void checkRecursive(Local l) {
		Set<Local> visited = new HashSet<>();
		
		Queue<Local> worklist = new LinkedList<>();
		worklist.add(l);
		
		while(!worklist.isEmpty()) {
			l = worklist.poll();
			AbstractCopyStmt copy = pool.defs.get(l);
			
			Set<Local> set = new HashSet<>();
			
			Expr rhs = copy.getExpression();
			if(rhs.getOpcode() == Opcode.LOCAL_LOAD) {
				set.add(((VarExpr) rhs).getLocal());
			} else if(rhs.getOpcode() == Opcode.PHI) {
				for(Expr e : ((PhiExpr) rhs).getArguments().values()) {
					set.add(((VarExpr) e).getLocal());
				}
			}
			
			for(Local v : set) {
				if(visited.contains(v)) {
					System.err.println(copy.getBlock().getGraph());
					System.err.printf("visited: %s%n", visited);
					System.err.printf(" copy: %s%n", copy);
					System.err.printf("  dup: %s%n", v);
					throw new RuntimeException();
				}
			}
			
			worklist.addAll(set);
			visited.addAll(set);
		}
	}
	
	@Override
	public TaintableSet<Expr> getValues(ControlFlowGraph cfg, Local l) {
		if(cfg.getLocals() != pool) {
			throw new UnsupportedOperationException();
		}
		
		AbstractCopyStmt copy = pool.defs.get(l);
		
		TaintableSet<Expr> set = new TaintableSet<>();
		if(copy.getOpcode() == Opcode.PHI_STORE) {
		
//				checkRecursive(l);

//				PhiExpr phi = ((CopyPhiStmt) copy).getExpression();
			/*for(Expr e : phi.getArguments().values()) {
				if(e.getOpcode() == Opcode.LOCAL_LOAD) {
					Local l2 = ((VarExpr) e).getLocal();
					
					if(l2 == l) {
						throw new RuntimeException(copy.toString());
					}
				}
			}*/
//				set.addAll(phi.getArguments().values());
		} else {
//				set.add(copy.getExpression());
		}
		
		set.add(copy.getExpression());
		return set;
	}
}
