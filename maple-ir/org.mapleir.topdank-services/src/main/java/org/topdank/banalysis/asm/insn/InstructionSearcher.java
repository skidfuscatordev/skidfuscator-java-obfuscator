package org.topdank.banalysis.asm.insn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public class InstructionSearcher implements Opcodes {
	
	protected Collection<AbstractInsnNode> insns;
	protected InstructionPattern pattern;
	
	protected List<AbstractInsnNode[]> matches;
	
	public InstructionSearcher(Collection<AbstractInsnNode> insns,
			InstructionPattern pattern) {
		this.insns = insns;
		this.pattern = pattern;
		matches = new ArrayList<AbstractInsnNode[]>();
	}

	public InstructionSearcher(InsnList insns, int[] opcodes) {
		this(insns, new InstructionPattern(opcodes));
	}
	
	public InstructionSearcher(InsnList insns, AbstractInsnNode[] ains) {
		this(insns, new InstructionPattern(ains));
	}
	
	public InstructionSearcher(InsnList insns, InstructionPattern pattern) {
		this.insns = Arrays.asList(insns.toArray());
		this.pattern = pattern;
		matches = new ArrayList<AbstractInsnNode[]>();
	}
	
	public boolean search() {
		for(AbstractInsnNode ain : insns) {
			//if (ain instanceof LineNumberNode || ain instanceof FrameNode || ain instanceof LabelNode)
			//	continue;
			if(ain.getOpcode() == -1)
				continue;
			if (pattern.accept(ain)) {
				matches.add(pattern.getLastMatch());
				pattern.resetMatch();
			}
		}
		return size() != 0;
	}
	
	public List<AbstractInsnNode[]> getMatches() {
		return matches;
	}
	
	public int size() {
		return matches.size();
	}
}
