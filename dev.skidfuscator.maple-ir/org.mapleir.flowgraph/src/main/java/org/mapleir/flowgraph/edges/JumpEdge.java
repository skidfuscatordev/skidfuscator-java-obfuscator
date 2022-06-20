package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.objectweb.asm.util.Printer;

public abstract class JumpEdge<N extends FastGraphVertex> extends AbstractFlowEdge<N> {
	
	public final int opcode;
	
	public JumpEdge(int type, N src, N dst, int opcode) {
		super(type, src, dst);
		this.opcode = opcode;
	}
	
	@Override
	public String toGraphString() {
		return Printer.OPCODES[opcode];
	}
	
	@Override
	public String toString() {
		return String.format("Jump[%s] #%s -> #%s", Printer.OPCODES[opcode], src.getDisplayName(), dst.getDisplayName());
	}
	
	@Override
	public String toInverseString() {
		return String.format("Jump[%s] #%s <- #%s", Printer.OPCODES[opcode], dst.getDisplayName(), src.getDisplayName());
	}
}
