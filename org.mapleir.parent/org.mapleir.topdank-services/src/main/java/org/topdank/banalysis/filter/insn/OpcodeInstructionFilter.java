package org.topdank.banalysis.filter.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.topdank.banalysis.filter.InstructionFilter;
import org.topdank.banalysis.filter.OpcodeFilter;

public abstract class OpcodeInstructionFilter implements InstructionFilter {
	
	protected OpcodeFilter opcodeFilter;
	
	public OpcodeInstructionFilter(OpcodeFilter opcodeFilter) {
		this.opcodeFilter = opcodeFilter;
	}
	
	@Override
	public abstract boolean accept(AbstractInsnNode t);
}