package org.topdank.banalysis.filter.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.topdank.banalysis.filter.InstructionFilter;
import org.topdank.banalysis.filter.OpcodeFilter;
import org.topdank.banalysis.filter.ZeroCancelIntegerFilter;

/**
 * @author Bibl (don't ban me pls)
 */
public class IntInstructionFilter implements InstructionFilter {

	private OpcodeFilter opcodeFilter;
	private ZeroCancelIntegerFilter numberFilter;
	
	public IntInstructionFilter(IntInsnNode iin) {
		opcodeFilter = new OpcodeFilter(iin.getOpcode());
		numberFilter = new ZeroCancelIntegerFilter(iin.operand);
	}
	
	/* (non-Javadoc)
	 * @see org.topdank.banalysis.filter.Filter#accept(java.lang.Object)
	 */
	@Override
	public boolean accept(AbstractInsnNode t) {
		if (!(t instanceof IntInsnNode))
			return false;
		if (!opcodeFilter.accept(t))
			return false;
		IntInsnNode fin = (IntInsnNode) t;
		if (!numberFilter.accept(fin.operand))
			return false;
		return true;
	}
}
