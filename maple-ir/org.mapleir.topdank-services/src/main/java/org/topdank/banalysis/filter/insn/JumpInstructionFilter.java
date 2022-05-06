package org.topdank.banalysis.filter.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.topdank.banalysis.filter.OpcodeFilter;

public class JumpInstructionFilter extends OpcodeInstructionFilter {
	
	public JumpInstructionFilter(int opcode) {
		super(new OpcodeFilter(opcode));
	}
	
	public JumpInstructionFilter(OpcodeFilter filter) {
		super(filter);
	}
	
	@Override
	public boolean accept(AbstractInsnNode t) {
		if (!(t instanceof JumpInsnNode))
			return false;
		return opcodeFilter.accept(t);
	}
}