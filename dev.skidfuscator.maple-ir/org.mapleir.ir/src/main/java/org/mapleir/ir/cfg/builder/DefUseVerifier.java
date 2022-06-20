package org.mapleir.ir.cfg.builder;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefUseVerifier implements Opcode {

	public static void verify(ControlFlowGraph cfg) {
		try {
			verify0(cfg);
		} catch(RuntimeException e) {
			System.err.println(cfg);
			throw e;
		}
	}
	
	public static void verify0(ControlFlowGraph cfg) {
		LocalsPool lp = cfg.getLocals();
		
		Map<Local, AbstractCopyStmt> defs = new HashMap<>();
		NullPermeableHashMap<VersionedLocal, Set<VarExpr>> uses = new NullPermeableHashMap<>(HashSet::new);
		
		for(BasicBlock b : cfg.vertices()) {
			for(Stmt stmt : b) {
				
				if(stmt.getOpcode() == Opcode.LOCAL_STORE || stmt.getOpcode() == Opcode.PHI_STORE) {
					AbstractCopyStmt copy = (AbstractCopyStmt) stmt;
					defs.put(copy.getVariable().getLocal(), copy);
				}
				
				for(Expr e : stmt.enumerateOnlyChildren()) {
					if(e.getOpcode() == Opcode.LOCAL_LOAD) {
						VarExpr v = (VarExpr) e;
						
						uses.getNonNull((VersionedLocal)v.getLocal()).add(v);
					}
				}
			}
		}
		
		{
			Set<Local> dlocals = new HashSet<>();
			dlocals.addAll(defs.keySet());
			dlocals.addAll(lp.defs.keySet());
			
			for(Local l : dlocals) {
				if(!defs.containsKey(l)) {
					throw new IllegalStateException("(other): def of " + l);
				}
				if(!lp.defs.containsKey(l)) {
					throw new IllegalStateException("(real): def of " + l);
				}
				
				AbstractCopyStmt copy1 = defs.get(l);
				AbstractCopyStmt copy2 = lp.defs.get(l);
				
				if(copy1 != copy2) {
					throw new IllegalStateException("dtest: " + copy1 + " :: " + copy2);
				}
			}
		}
		
		{
			Set<VersionedLocal> ulocals = new HashSet<>();
			ulocals.addAll(uses.keySet());
			ulocals.addAll(lp.uses.keySet());
			
			for(VersionedLocal l : ulocals) {
				/*if(!uses.containsKey(l)) {
					throw new IllegalStateException("(other): use of " + l);
				}
				
				if(!lp.uses.containsKey(l)) {
					throw new IllegalStateException("(real): use of " + l);
				}*/
				
				Set<VarExpr> uses1 = uses.get(l);
				Set<VarExpr> uses2 = lp.uses.get(l);
				
				if(uses1 == null) {
					if(uses2.size() != 0) {
						throw new IllegalStateException(String.format("utest1: %s, u1:null :: u2:%d", l, uses2.size()));		
					}
				} else if(uses2 == null) {
					if(uses1.size() == 0) {
						throw new IllegalStateException(String.format("utest2: %s, u1:%d :: u2:null", l, uses1.size()));
					}
				} else {
					if(uses2.size() != uses1.size()) {
						throw new IllegalStateException(String.format("utest3: %s, u1:%d :: u2:%d", l, uses1.size(), uses2.size()));
					}
				}
			}
		}
	}
}