package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class ConditionalJumpEdge<N extends FastGraphVertex> extends JumpEdge<N> {
	
	public ConditionalJumpEdge(N src, N dst, int opcode) {
		super(COND, src, dst, opcode);
	}
	
	@Override
	public String toString() {
		return "Conditional" + super.toString();
	}
	
	@Override
	public String toInverseString() {
		return "Conditional" + super.toInverseString();
	}
	
	@Override
	public ConditionalJumpEdge<N> clone(N src, N dst) {
		return new ConditionalJumpEdge<>(src, dst, opcode);
	}
}