package org.topdank.banalysis.filter.insn;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.topdank.banalysis.filter.Filter;
import org.topdank.banalysis.filter.InstructionFilter;
import org.topdank.banalysis.filter.OpcodeFilter;

public class SwitchInstructionFilter implements InstructionFilter {
	
	protected OpcodeFilter opcodeFilter;
	protected List<Filter<Object>> filters;
	
	public SwitchInstructionFilter(int opcode) {
		this(new OpcodeFilter(opcode));
	}
	
	public SwitchInstructionFilter(int opcode, List<Filter<Object>> filters) {
		this(new OpcodeFilter(opcode), filters);
	}
	
	public SwitchInstructionFilter(OpcodeFilter opcodeFilter) {
		this(opcodeFilter, new ArrayList<Filter<Object>>());
	}
	
	public SwitchInstructionFilter(OpcodeFilter opcodeFilter, List<Filter<Object>> filters) {
		this.opcodeFilter = opcodeFilter;
		this.filters = filters;
	}
	
	public SwitchInstructionFilter addFilter(Filter<Object> filter) {
		filters.add(filter);
		return this;
	}
	
	@Override
	public boolean accept(AbstractInsnNode t) {
		if (!(t instanceof LookupSwitchInsnNode || t instanceof TableSwitchInsnNode))
			return false;
		if (!opcodeFilter.accept(t))
			return false;
		for(Filter<Object> filter : filters) {
			if (!filter.accept(t))
				return false;
		}
		return true;
	}
}