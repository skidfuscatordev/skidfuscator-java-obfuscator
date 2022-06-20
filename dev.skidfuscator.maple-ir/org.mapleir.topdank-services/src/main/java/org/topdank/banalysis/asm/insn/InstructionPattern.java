package org.topdank.banalysis.asm.insn;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.topdank.banalysis.filter.InstructionFilter;
import org.topdank.banalysis.filter.OpcodeFilter;
import org.topdank.banalysis.filter.insn.FieldInstructionFilter;
import org.topdank.banalysis.filter.insn.IincInstructionFilter;
import org.topdank.banalysis.filter.insn.InsnInstructionFilter;
import org.topdank.banalysis.filter.insn.IntInstructionFilter;
import org.topdank.banalysis.filter.insn.JumpInstructionFilter;
import org.topdank.banalysis.filter.insn.LdcInstructionFilter;
import org.topdank.banalysis.filter.insn.MethodInstructionFilter;
import org.topdank.banalysis.filter.insn.MultiANewArrayInstructionFilter;
import org.topdank.banalysis.filter.insn.TypeInstructionFilter;
import org.topdank.banalysis.filter.insn.VarInstructionFilter;

/**
 * Pattern filter holder and stepper.
 * @author Bibl
 * 
 */
public class InstructionPattern implements Opcodes {
	
	/** Last instruction-match position pointer **/
	protected int pointer;
	/** Filters/patterns/search criteria. **/
	protected InstructionFilter[] filters;
	/** Last match found cache. **/
	protected AbstractInsnNode[] lastMatch;
	
	/**
	 * Construct a new pattern from the specified instructions.
	 * @param insns {@link AbstractInsnNode} pattern array.
	 */
	public InstructionPattern(AbstractInsnNode[] insns) {
		filters = translate(insns);
		lastMatch = new AbstractInsnNode[insns.length];
	}
	
	/**
	 * Construct a new pattern from the specified opcode.
	 * @param opcodes Opcodes to convert to {@link OpcodeFilter}s.
	 */
	public InstructionPattern(int[] opcodes) {
		filters = new InstructionFilter[opcodes.length];
		lastMatch = new AbstractInsnNode[opcodes.length];
		for(int i = 0; i < opcodes.length; i++) {
			filters[i] = new OpcodeFilter(opcodes[i]);
		}
	}
	
	/**
	 * Construct an absolute pattern from user-defined filters.
	 * @param filters User-defined {@link InstructionFilter}s.
	 */
	public InstructionPattern(InstructionFilter[] filters) {
		this.filters = filters;
		lastMatch = new AbstractInsnNode[filters.length];
	}
	
	/**
	 * Steps through the instruction list checking if the current instruction ended a successful pattern-match sequence.
	 * @param ain {@link AbstractInsnNode} to check.
	 * @return True if this instruction successfully completed the pattern.
	 */
	public boolean accept(AbstractInsnNode ain) {
		if (pointer >= filters.length)
			reset();
		
		InstructionFilter filter = filters[pointer];
		if (filter.accept(ain)) {
			lastMatch[pointer] = ain;
			if (pointer >= (filters.length - 1)) {
				return true;
			}
			pointer++;
		} else {
			reset();
		}
		return false;
	}
	
	/**
	 * @return Last pattern sequence match equivilent from the inputted {@link AbstractInsnNode}s.
	 */
	public AbstractInsnNode[] getLastMatch() {
		return lastMatch;
	}
	
	/**
	 * Resets the instruction pointer and clears the last match cache data.
	 */
	public void resetMatch() {
		reset();
		AbstractInsnNode[] match = lastMatch;
		lastMatch = new AbstractInsnNode[match.length];
	}
	
	/**
	 * Sets the current instruction pointer to 0 (start of pattern).
	 */
	public void reset() {
		pointer = 0;
	}
	
	/**
	 * Converts an array of {@link AbstractInsnNode}s to their {@link InstructionFilter} counterparts.
	 * @param ains {@link AbstractInsnNode}s to convert.
	 * @return Array of {@link InstructionFilter}s.
	 */
	public static InstructionFilter[] translate(AbstractInsnNode[] ains) {
		InstructionFilter[] filters = new InstructionFilter[ains.length];
		for(int i = 0; i < ains.length; i++) {
			filters[i] = translate(ains[i]);
		}
		return filters;
	}
	
	/**
	 * Translate a single {@link AbstractInsnNode} to an {@link InstructionFilter}.
	 * @param ain Instruction to convert.
	 * @return A filter an an equivilent to the inputted instruction.
	 */
	public static InstructionFilter translate(AbstractInsnNode ain) {
		if (ain instanceof LdcInsnNode) {
			return new LdcInstructionFilter(((LdcInsnNode) ain).cst);
		} else if (ain instanceof TypeInsnNode) {
			return new TypeInstructionFilter(ain.getOpcode(), ((TypeInsnNode) ain).desc);
		} else if (ain instanceof FieldInsnNode) {
			return new FieldInstructionFilter(ain.getOpcode(), ((FieldInsnNode) ain).owner, ((FieldInsnNode) ain).name, ((FieldInsnNode) ain).desc);
		} else if (ain instanceof MethodInsnNode) {
			return new MethodInstructionFilter(ain.getOpcode(), ((MethodInsnNode) ain).owner, ((MethodInsnNode) ain).name, ((MethodInsnNode) ain).desc);
		} else if (ain instanceof VarInsnNode) {
			return new VarInstructionFilter(ain.getOpcode(), ((VarInsnNode) ain).var);
		} else if (ain instanceof InsnNode) {
			return new InsnInstructionFilter(ain.getOpcode());
		} else if (ain instanceof IincInsnNode) {
			return new IincInstructionFilter(((IincInsnNode) ain).incr, ((IincInsnNode) ain).var);
		} else if (ain instanceof JumpInsnNode) {
			return new JumpInstructionFilter(ain.getOpcode());
		} else if (ain instanceof LabelNode) {
			return InstructionFilter.ACCEPT_ALL; // TODO: Cache labels and check. // TODO: That's a fucking stupid idea.
		} else if (ain instanceof MultiANewArrayInsnNode) {
			return new MultiANewArrayInstructionFilter(((MultiANewArrayInsnNode) ain).desc, ((MultiANewArrayInsnNode) ain).dims);
		} else if(ain instanceof IntInsnNode) {
			return new IntInstructionFilter((IntInsnNode) ain);
		} else {
			return InstructionFilter.ACCEPT_ALL;
		}
	}
}
