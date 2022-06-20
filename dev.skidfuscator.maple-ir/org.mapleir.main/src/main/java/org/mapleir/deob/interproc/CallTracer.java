package org.mapleir.deob.interproc;

import java.util.HashSet;
import java.util.Set;

import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.InvocationResolver;
import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.asm.MethodNode;

public abstract class CallTracer {

	protected final ApplicationClassSource tree;
	protected final InvocationResolver resolver;
	
	private final Set<MethodNode> visited;
	
	public CallTracer(ApplicationClassSource tree, InvocationResolver resolver) {
		this.tree = tree;
		this.resolver = resolver;
		
		visited = new HashSet<>();
	}
	
	public void trace(MethodNode m) {
		if(visited.contains(m)) {
			return;
		} else {
			visited.add(m);
			visitMethod(m);
			
			if(tree.isLibraryClass(m.getOwner())) {
				return;
			}
		}
		
		traceImpl(m);
	}
	
	protected abstract void traceImpl(MethodNode m);
	
	protected void visitMethod(MethodNode m) {}
	
	protected void processedInvocation(MethodNode caller, MethodNode callee, Invocation call) {}
}
