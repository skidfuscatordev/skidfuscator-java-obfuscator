package org.topdank.banalysis.filter;

import org.objectweb.asm.tree.AbstractInsnNode;

public class OpcodeFilter implements InstructionFilter {
	
	protected int opcode;
	
	public OpcodeFilter(int opcode) {
		this.opcode = opcode;
	}
	
	@Override
	public boolean accept(AbstractInsnNode t) {
		if (opcode == -1)
			return true;
		return opcode == t.getOpcode();
	}
}
