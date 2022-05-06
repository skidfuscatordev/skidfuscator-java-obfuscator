package org.mapleir.deob.passes;

import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ArithmeticExpr.Operator;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt.ComparisonType;
import org.mapleir.asm.MethodNode;

public class ConstantExpressionReorderPass implements IPass, Opcode {

	@Override
	public String getId() {
		return "CESwap";
	}


	@Override
	public PassResult accept(PassContext pcxt) {
		AnalysisContext cxt = pcxt.getAnalysis();
		int delta = 0;
		for(MethodNode m : cxt.getIRCache().getActiveMethods()) {
			ControlFlowGraph ir = cxt.getIRCache().getFor(m);
			delta += transform(ir);
		}
		System.out.println("  swapped " + delta + " constant expression orders.");
		return PassResult.with(pcxt, this).finished(delta).make();
	}
	
	public int transform(ControlFlowGraph ir) {
		int i = 0;
		
		for(BasicBlock b : ir.vertices()) {
			for(Stmt stmt : b) {
				if(stmt.getOpcode() == COND_JUMP) {
					ConditionalJumpStmt cjs = (ConditionalJumpStmt) stmt;
					Expr r = cjs.getRight();
					Expr l = cjs.getLeft();
					
					ComparisonType type = cjs.getComparisonType();
					if(type == ComparisonType.EQ || type == ComparisonType.NE) {
						if(shouldReorder(r, l)) {
							cjs.setRight(null);
							cjs.setLeft(null);
							cjs.setLeft(r);
							cjs.setRight(l);
							i++;
						}
					}
				}
				
				for(Expr e : stmt.enumerateOnlyChildren()) {
					if(e.getOpcode() == ARITHMETIC) {
						ArithmeticExpr arith = (ArithmeticExpr) e;
						Expr r = arith.getRight();
						Expr l = arith.getLeft();
						
						Operator op = arith.getOperator();
						if(!op.doesOrderMatter()) {
							if(shouldReorder(r, l)) {
								arith.setRight(null);
								arith.setLeft(null);
								arith.setLeft(r);
								arith.setRight(l);
								i++;
							}
						}
					}
				}
			}
		}
		
		return i;
	}
	
	private boolean shouldReorder(Expr r, Expr l) {
		return (l.getOpcode() == CONST_LOAD) && (r.getOpcode() != CONST_LOAD);
	}
}
