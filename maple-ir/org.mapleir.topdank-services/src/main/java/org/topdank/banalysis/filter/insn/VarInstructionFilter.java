package org.topdank.banalysis.filter.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.topdank.banalysis.filter.IntegerFilter;
import org.topdank.banalysis.filter.OpcodeFilter;
import org.topdank.banalysis.filter.ZeroCancelIntegerFilter;

public class VarInstructionFilter extends OpcodeInstructionFilter {
	
	private IntegerFilter varFilter;
	
	public VarInstructionFilter() {
		this(-1, -1);
	}
	
	public VarInstructionFilter(OpcodeFilter filter, IntegerFilter filter1) {
		super(filter);
		varFilter = filter1;
	}
	
	public VarInstructionFilter(int opcode, int var) {
		this(new OpcodeFilter(opcode), new ZeroCancelIntegerFilter(var));
	}
	
	@Override
	public boolean accept(AbstractInsnNode t) {
		if (!(t instanceof VarInsnNode))
			return false;
		VarInsnNode vin = (VarInsnNode) t;
		return opcodeFilter.accept(vin) && varFilter.accept(vin.var);
	}
}