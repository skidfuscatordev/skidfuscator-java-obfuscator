package org.mapleir.deob.callgraph;

import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.callgraph.CallGraphEdge.FunctionOwnershipEdge;
import org.mapleir.deob.callgraph.CallGraphEdge.SiteInvocationEdge;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.stdlib.collections.list.Worklist;
import org.mapleir.asm.MethodNode;

import java.util.Collection;
import java.util.Set;

public class SensitiveCallGraphBuilder implements Worklist.Worker<MethodNode> {
	private final CallSiteSensitiveCallGraph callGraph;
	private final AnalysisContext context;
	private final Worklist<MethodNode> worklist;

	public SensitiveCallGraphBuilder(AnalysisContext context) {
		callGraph = new CallSiteSensitiveCallGraph();
		this.context = context;
		worklist = makeWorklist();
	}

	public Worklist<MethodNode> getWorklist() {
		return worklist;
	}

	public CallSiteSensitiveCallGraph build(Collection<MethodNode> entries) {
		getWorklist().queueData(entries);
		worklist.processQueue();
		return callGraph;
	}

	protected Worklist<MethodNode> makeWorklist() {
		Worklist<MethodNode> worklist = new Worklist<>();
		worklist.addWorker(this);
		return worklist;
	}

	@Override
	public void process(Worklist<MethodNode> worklist, MethodNode n) {
		if (worklist != this.worklist) {
			throw new IllegalStateException();
		}

		if (worklist.hasProcessed(n)) {
			throw new UnsupportedOperationException(String.format("Already processed %s", n));
		}

		/* this is not the same as getNode */
		CallGraphNode.CallReceiverNode currentReceiverNode = createNode(n, false);

		ControlFlowGraph cfg = context.getIRCache().get(n);

		if (cfg == null) {
			return;
		}

		for (Stmt stmt : cfg.stmts()) {
			for (Expr e : stmt.enumerateOnlyChildren()) {
				if (e instanceof Invocation) {
					Invocation invoke = (Invocation) e;

					CallGraphNode.CallSiteNode thisCallSiteNode = callGraph.addInvocation(n, invoke);

					/* link the current receiver to this call site. */
					FunctionOwnershipEdge foe = new FunctionOwnershipEdge(currentReceiverNode, thisCallSiteNode);
					callGraph.addEdge(foe);

					Set<MethodNode> targets = invoke.resolveTargets(context.getInvocationResolver());
					
					for (MethodNode target : targets) {
						CallGraphNode.CallReceiverNode targetReceiverNode = createNode(target, true);

						/* link each target to the call site. */
						SiteInvocationEdge sie = new SiteInvocationEdge(thisCallSiteNode, targetReceiverNode);
						callGraph.addEdge(sie);
					}
				}
			}
		}
	}

	/*
	 * either get a pre built node or make one and add it to the worklist.
	 */
	protected CallGraphNode.CallReceiverNode createNode(MethodNode m, boolean queue) {
		if (callGraph.containsMethod(m)) {
			return callGraph.getNode(m);
		} else {
			CallGraphNode.CallReceiverNode currentReceiverNode = callGraph.addMethod(m);
			if (queue) {
				worklist.queueData(m);
			}
			return currentReceiverNode;
		}
	}
}
