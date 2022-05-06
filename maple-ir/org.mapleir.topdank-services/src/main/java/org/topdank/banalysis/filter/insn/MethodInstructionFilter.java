package org.topdank.banalysis.filter.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.topdank.banalysis.filter.ConstantFilter;
import org.topdank.banalysis.filter.OpcodeFilter;

public class MethodInstructionFilter extends OpcodeInstructionFilter {
	
	private ConstantFilter<String> ownerFilter;
	private ConstantFilter<String> nameFilter;
	private ConstantFilter<String> descFilter;
	
	public MethodInstructionFilter(OpcodeFilter filter, String owner, String name, String desc) {
		super(filter);
		ownerFilter = createFilter(owner);
		nameFilter = createFilter(name);
		descFilter = createFilter(desc);
	}
	
	public MethodInstructionFilter(int opcode, String owner, String name, String desc) {
		super(new OpcodeFilter(opcode));
		ownerFilter = createFilter(owner);
		nameFilter = createFilter(name);
		descFilter = createFilter(desc);
	}
	
	private ConstantFilter<String> createFilter(String s) {
		if (s == null)
			return new ConstantFilter<String>();
		return new ConstantFilter<String>(s);
	}
	
	@Override
	public boolean accept(AbstractInsnNode t) {
		if (!(t instanceof MethodInsnNode))
			return false;
		if (!opcodeFilter.accept(t))
			return false;
		MethodInsnNode min = (MethodInsnNode) t;
		if (!ownerFilter.accept(min.owner))
			return false;
		if (!nameFilter.accept(min.name))
			return false;
		if (!descFilter.accept(min.desc))
			return false;
		return true;
	}
}