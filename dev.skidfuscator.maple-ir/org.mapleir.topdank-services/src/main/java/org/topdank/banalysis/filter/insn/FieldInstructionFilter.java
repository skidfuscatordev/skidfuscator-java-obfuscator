package org.topdank.banalysis.filter.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.topdank.banalysis.filter.ConstantFilter;
import org.topdank.banalysis.filter.OpcodeFilter;

public class FieldInstructionFilter extends OpcodeInstructionFilter {
	
	private ConstantFilter<String> ownerFilter;
	private ConstantFilter<String> nameFilter;
	private ConstantFilter<String> descFilter;
	
	public FieldInstructionFilter(OpcodeFilter filter, String owner, String name, String desc) {
		super(filter);
		ownerFilter = createFilter(owner);
		nameFilter = createFilter(name);
		descFilter = createFilter(desc);
	}
	
	public FieldInstructionFilter(int opcode, String owner, String name, String desc) {
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
		if (!(t instanceof FieldInsnNode))
			return false;
		if (!opcodeFilter.accept(t))
			return false;
		FieldInsnNode fin = (FieldInsnNode) t;
		if (!ownerFilter.accept(fin.owner))
			return false;
		if (!nameFilter.accept(fin.name))
			return false;
		if (!descFilter.accept(fin.desc))
			return false;
		return true;
	}
}