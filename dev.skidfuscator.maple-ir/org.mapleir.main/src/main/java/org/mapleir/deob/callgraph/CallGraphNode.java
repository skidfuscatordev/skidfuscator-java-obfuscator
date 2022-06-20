package org.mapleir.deob.callgraph;

import org.mapleir.ir.code.Expr;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.asm.MethodNode;

public abstract class CallGraphNode implements FastGraphVertex {

	private final int id;

	public CallGraphNode(int id) {
		this.id = id;
	}

	@Override
	public String getDisplayName() {
		return Integer.toString(id);
	}

	@Override
	public int getNumericId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CallGraphNode that = (CallGraphNode) o;

		return id == that.id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public abstract String toString();

	// A call receiver; e.g. a MethodNode.
	// TODO: why don't we just use a methodnode instead directly?
	public static class CallReceiverNode extends CallGraphNode {

		private final MethodNode method;

		public CallReceiverNode(int id, MethodNode method) {
			super(id);
			this.method = method;
		}

		@Override
		public String toString() {
			return method.toString();
		}
	}
	
	// A call site; e.g. an invocation.
	public static class CallSiteNode extends CallGraphNode {

		private final MethodNode sourceMethod;
		private final Expr invoke;
		
		public CallSiteNode(int id, MethodNode sourceMethod, Expr invoke) {
			super(id);
			this.sourceMethod = sourceMethod;
			this.invoke = invoke;
		}

		@Override
		public String toString() {
			return sourceMethod.owner + "." + sourceMethod.getName() + "@" + invoke.getBlock().indexOf(invoke.getRootParent()) + ":" + invoke.getParent().indexOf(invoke);
		}
	}
}
