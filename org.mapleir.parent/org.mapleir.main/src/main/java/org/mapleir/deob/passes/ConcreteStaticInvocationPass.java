package org.mapleir.deob.passes;

import org.mapleir.app.service.InvocationResolver;
import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public class ConcreteStaticInvocationPass implements IPass {

	@Override
	public PassResult accept(PassContext pcxt) {
		AnalysisContext cxt = pcxt.getAnalysis();
		int fixed = 0;
		
		InvocationResolver resolver = cxt.getInvocationResolver();
		
		for(ClassNode cn : cxt.getApplication().iterate()) {
			for(MethodNode mn : cn.getMethods()) {
				ControlFlowGraph cfg = cxt.getIRCache().getFor(mn);
				
				for(BasicBlock b : cfg.vertices()) {
					for(Stmt stmt : b) {
						for(Expr e : stmt.enumerateOnlyChildren()) {
							if(e.getOpcode() == Opcode.INVOKE) {
								InvocationExpr invoke = (InvocationExpr) e;
								
								if(invoke.getCallType() == InvocationExpr.CallType.STATIC) {
									MethodNode invoked = resolver.resolveStaticCall(invoke.getOwner(), invoke.getName(), invoke.getDesc());
									
									if(invoked != null) {
										if(!invoked.getOwner().equals(invoke.getOwner())) {
											invoke.setOwner(invoked.getOwner());
											fixed++;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		System.out.printf("  corrected %d dodgy static calls.%n", fixed);
		
		return PassResult.with(pcxt, this).finished().make();
	}
}
