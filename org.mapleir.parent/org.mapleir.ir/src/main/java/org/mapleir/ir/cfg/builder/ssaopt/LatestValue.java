package org.mapleir.ir.cfg.builder.ssaopt;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.impl.VersionedLocal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LatestValue {
	
	public static final int PARAM = 0, PHI = 1, CONST = 2, VAR = 3;
	private static final String[] TO_STRING = new String[]{"param", "phi", "phi", "other"};
	
	private final ControlFlowGraph cfg;
	private final int type;
	private final Object rvalue;
	private final Object svalue;
	private final VersionedLocal src;
	private final List<Constraint> constraints;
	
	public LatestValue(ControlFlowGraph cfg, int type, Object val, VersionedLocal src) {
		this(cfg, type, val, val, src);
	}
	
	public LatestValue(ControlFlowGraph cfg, int type, Object rvalue, Object svalue, VersionedLocal src) {
		this.cfg = cfg;
		this.type = type;
		this.rvalue = rvalue;
		this.svalue = svalue;
		this.src = src;
		constraints = new ArrayList<>();
	}
	
	public List<Constraint> getConstraints() {
		return constraints;
	}
	
	public boolean hasConstraints() {
		return !constraints.isEmpty();
	}
	
	public int getType() {
		return type;
	}
	
	public Object getRealValue() {
		return rvalue;
	}
	
	public Object getSuggestedValue() {
		return svalue;
	}
	
	public VersionedLocal getSource() {
		return src;
	}
	
	public void importConstraints(LatestValue v) {
		constraints.addAll(v.constraints);
	}
	
	public void makeConstraints(Expr e) {
		for(Expr s : e.enumerateWithSelf()) {
			int op = s.getOpcode();
			if(op == Opcode.FIELD_LOAD) {
				FieldConstraint c = new FieldConstraint((FieldLoadExpr) s);
				constraints.add(c);
			} else if(ConstraintUtil.isInvoke(op)) {
				constraints.add(new InvokeConstraint());
			} else if(op == Opcode.ARRAY_LOAD) {
				constraints.add(new ArrayConstraint());
			}
		}
	}
	
	private Set<Stmt> findReachable(Stmt from, Stmt to) {
		Set<Stmt> res = new HashSet<>();
		BasicBlock f = from.getBlock();
		BasicBlock t = to.getBlock();
		
		int end = f == t ? f.indexOf(to) : f.size();
		for(int i=f.indexOf(from); i < end; i++) {
			res.add(f.get(i));
		}
		
		if(f != t) {
			for(BasicBlock r : cfg.dfsNoHandlers(f, t)) {
				res.addAll(r);
			}
		}
		
		return res;
	}

	public boolean canPropagate(AbstractCopyStmt def, Stmt use, Expr tail, boolean debug) {
		Local local = def.getVariable().getLocal();
		
		Set<Stmt> path = findReachable(def, use);
		path.remove(def);
		path.add(use);
		
		if(debug) {
			System.out.println();
			System.out.println("from " + def);
			System.out.println("to " + use);
			System.out.println(this);
			System.out.println("constraints: " + constraints.size());
			for(Constraint c : constraints) {
				System.out.println(" " + c);
			}
			
			System.out.println(" path:");
			for(Stmt s : path) {
				System.out.println("  " + s);
			}
		}
		
		for(Stmt stmt : path) {
			if(stmt != use) {
				for(CodeUnit s : stmt.enumerateWithSelf()) {
					for(Constraint c : constraints) {
						if(c.fails(s)) {
							if(debug) {
								System.out.println("  Fail: " + c);
								System.out.println("  stmt: " + stmt);
								System.out.println("     c: " + s);
							}
							return false;
						}
					}
				}
			} else {
				if(constraints.size() > 0) {
					for(CodeUnit s : stmt.enumerateExecutionOrder()) {
						if(s == tail && (s.getOpcode() == Opcode.LOCAL_LOAD && ((VarExpr) s).getLocal() == local)) {
							break;
						} else {
							for(Constraint c : constraints) {
								if(c.fails(s)) {
									if(debug) {
										System.out.println("  Fail: " + c);
										System.out.println("  stmt: " + stmt);
										System.out.println("     c: " + s);
									}
									return false;
								}
							}
						}
					}
				}
			}
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("LatestValue: {type=%s, rval=%s, sval=%s, src=%s, cons=%d}", TO_STRING[type], rvalue, svalue, src, constraints.size());
	}
}