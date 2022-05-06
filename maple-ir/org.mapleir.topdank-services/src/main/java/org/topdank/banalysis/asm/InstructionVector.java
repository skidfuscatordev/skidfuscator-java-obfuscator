package org.topdank.banalysis.asm;

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.topdank.banalysis.filter.Filter;

public class InstructionVector extends InfoVector<AbstractInsnNode> {
	
	public InstructionVector(List<AbstractInsnNode> insns) {
		super(insns);
	}
	
	public InstructionVector(List<AbstractInsnNode> insns, boolean definiteCount, Filter<AbstractInsnNode> filter) {
		super(insns, definiteCount, filter);
	}
}