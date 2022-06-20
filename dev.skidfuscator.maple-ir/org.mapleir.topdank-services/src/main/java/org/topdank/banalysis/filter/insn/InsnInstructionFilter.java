package org.topdank.banalysis.filter.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.topdank.banalysis.filter.OpcodeFilter;

public class InsnInstructionFilter extends OpcodeInstructionFilter {
	
	public InsnInstructionFilter(int opcode) {
		super(new OpcodeFilter(opcode));
	}
	
	public InsnInstructionFilter(OpcodeFilter filter) {
		super(filter);
	}
	
	@Override
	public boolean accept(AbstractInsnNode t) {
		if (!(t instanceof InsnNode))
			return false;
		return opcodeFilter.accept(t);
	}
}