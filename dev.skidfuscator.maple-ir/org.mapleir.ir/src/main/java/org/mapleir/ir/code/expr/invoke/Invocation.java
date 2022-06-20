package org.mapleir.ir.code.expr.invoke;

import java.util.Set;

import org.mapleir.app.service.InvocationResolver;
import org.mapleir.ir.code.Expr;
import org.mapleir.asm.MethodNode;

/* Definitions:
 *   parameterExprs:= the Exprs that are actually passed to 
 *                    the receiver object, i.e excluding the
 *                    receiver.
 *   argumentExprs:= the Exprs that are both virtually and
 *                   physically passed during the invocation,
 *                   i.e. including the receiver.
 *   physicalReceiver:= a receiver object on which a method is
 *                      called that may be acquired between
 *                      before the call. */
public abstract class Invocation extends Expr implements Invokable {
	
	public Invocation(int opcode) {
		super(opcode);
	}

	public abstract boolean isStatic();
	
	public abstract boolean isDynamic();
	
	public abstract Expr getPhysicalReceiver();
	
	public abstract Set<MethodNode> resolveTargets(InvocationResolver res);
}
