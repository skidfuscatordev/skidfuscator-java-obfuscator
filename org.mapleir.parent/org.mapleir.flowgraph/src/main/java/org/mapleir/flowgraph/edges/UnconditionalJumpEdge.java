package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.objectweb.asm.Opcodes;

public class UnconditionalJumpEdge<N extends FastGraphVertex> extends JumpEdge<N> {
	
	public UnconditionalJumpEdge(N src, N dst) {
		super(UNCOND, src, dst, Opcodes.GOTO);
	}

	@Override
	public String toString() {
		return "Unconditional" + super.toString();
	}
	
	@Override
	public String toInverseString() {
		return "Unconditional" + super.toInverseString();
	}
	
	@Override
	public UnconditionalJumpEdge<N> clone(N src, N dst) {
		return new UnconditionalJumpEdge<>(src, dst);
	}
}