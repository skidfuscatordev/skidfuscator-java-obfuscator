package org.mapleir.deob.callgraph;

import org.mapleir.deob.callgraph.CallGraphNode.CallReceiverNode;
import org.mapleir.deob.callgraph.CallGraphNode.CallSiteNode;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphEdgeImpl;

public interface CallGraphEdge extends FastGraphEdge<CallGraphNode> {
	boolean canClone(CallGraphNode src, CallGraphNode dst);

	CallGraphEdge clone(CallGraphNode src, CallGraphNode dst);

	// The source receiver (method) contains the destination call site (invocation).
	class FunctionOwnershipEdge extends FastGraphEdgeImpl<CallGraphNode> implements CallGraphEdge {
		public FunctionOwnershipEdge(CallReceiverNode src, CallSiteNode dst) {
			super(src, dst);
		}

		@Override
		public boolean canClone(CallGraphNode src, CallGraphNode dst) {
			return src instanceof CallReceiverNode && dst instanceof CallSiteNode;
		}

		@Override
		public CallGraphEdge clone(CallGraphNode src, CallGraphNode dst) {
			return new FunctionOwnershipEdge((CallReceiverNode) src, (CallSiteNode) dst);
		}
	}

	// The source call site (invocation) resolves to the destination method
	// (receiver).
	class SiteInvocationEdge extends FastGraphEdgeImpl<CallGraphNode> implements CallGraphEdge {

		public SiteInvocationEdge(CallSiteNode src, CallReceiverNode dst) {
			super(src, dst);
		}

		@Override
		public boolean canClone(CallGraphNode src, CallGraphNode dst) {
			return src instanceof CallSiteNode && dst instanceof CallReceiverNode;
		}

		@Override
		public CallGraphEdge clone(CallGraphNode src, CallGraphNode dst) {
			return new SiteInvocationEdge((CallSiteNode) src, (CallReceiverNode) dst);
		}

	}
}