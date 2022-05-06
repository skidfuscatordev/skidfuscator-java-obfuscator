package org.topdank.banalysis.filter.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.topdank.banalysis.filter.ConstantFilter;
import org.topdank.banalysis.filter.InstructionFilter;
import org.topdank.banalysis.filter.IntegerFilter;
import org.topdank.banalysis.filter.ZeroCancelIntegerFilter;

public class MultiANewArrayInstructionFilter implements InstructionFilter {
	
	protected ConstantFilter<String> descFilter;
	protected IntegerFilter dimsFilter;
	
	public MultiANewArrayInstructionFilter(String desc, int dims) {
		descFilter = new ConstantFilter<String>(desc);
		dimsFilter = new ZeroCancelIntegerFilter(dims);
	}
	
	@Override
	public boolean accept(AbstractInsnNode t) {
		if (!(t instanceof MultiANewArrayInsnNode))
			return false;
		return descFilter.accept(((MultiANewArrayInsnNode) t).desc) && dimsFilter.accept(((MultiANewArrayInsnNode) t).dims);
	}
}