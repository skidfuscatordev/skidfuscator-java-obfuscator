package org.topdank.banalysis.filter;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface InstructionFilter extends Filter<AbstractInsnNode> {
	
	public static final InstructionFilter ACCEPT_ALL = new InstructionFilter() {
		@Override
		public boolean accept(AbstractInsnNode t) {
			return true;
		}
	};
}