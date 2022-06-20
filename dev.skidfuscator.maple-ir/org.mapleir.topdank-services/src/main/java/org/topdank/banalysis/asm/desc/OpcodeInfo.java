package org.topdank.banalysis.asm.desc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.mapleir.asm.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.topdank.banalysis.filter.InstructionFilter;

/**
 * Utility class for generic bytecode instructions and opcodes.
 * @author Bibl
 *
 */
public class OpcodeInfo implements Opcodes {
	
	/** A map of numerical opcodes to their descs. **/
	public static final HashMap<Integer, String> OPCODES;
	/** A map of named opcodes to their numerical equivalents. **/
	public static final HashMap<String, Integer> OPCODE_NAMES;
	
	/** Used for getting the opcode for loading a value on the stack in a method. **/
	public static final int TYPE_LOAD = 1;
	/** Used for getter the opcode for returning a value from a method. **/
	public static final int TYPE_RETURN = 2;
	
	static {
		OPCODES = new HashMap<Integer, String>();
		OPCODE_NAMES = new HashMap<String, Integer>();
		try {
			ClassReader cr = new ClassReader("org.objectweb.asm.Opcodes");
			ClassNode cn = new ClassNode();
			cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			ListIterator<?> fnIt = cn.fields.listIterator();
			while (fnIt.hasNext()) {
				Object fO = fnIt.next();
				FieldNode f = (FieldNode) fO;
				try {
					OPCODES.put((Integer) f.node.value, f.getName());
					OPCODE_NAMES.put(f.getName(), (Integer) f.node.value);
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Converts a string of named opcodes to the integer equivalents.
	 * @param opcodeString String of named opcodes.
	 * @return Array of opcodes.
	 */
	public static final int[] toOpcodes(String opcodeString) {
		String[] stringOpcodes = opcodeString.split(" ");
		int[] opcodes = new int[stringOpcodes.length];
		for (int i = 0; i < stringOpcodes.length; i++) {
			opcodes[i] = OPCODE_NAMES.get(stringOpcodes[i]);
		}
		return opcodes;
	}
	
	/**
	 * Convert instructions to a string.
	 * @param ains Instructions to convert
	 * @return String of opcodes.
	 */
	public static final String opcodesToString(AbstractInsnNode[] ains) {
		String[] ops = new String[ains.length];
		for (int i = 0; i < ains.length; i++) {
			ops[i] = OPCODES.get(ains[i].getOpcode());
		}
		return Arrays.toString(ops).replace("[", "").replace("]", "");
	}
	
	/**
	 * Gets the associated opcode for returning a value in a method
	 * @param typeDesc Desc of the field
	 * @return The opcode used for returning the field type
	 */
	public static int getReturnOpcode(String typeDesc) {
		return getOpcode(typeDesc, TYPE_RETURN);
	}
	
	/**
	 * Gets the associated opcode for loading a value on the stack in a method
	 * @param typeDesc Desc of the field
	 * @return The opcode used for loading the field type
	 */
	public static int getLoadOpcode(String typeDesc) {
		return getOpcode(typeDesc, TYPE_LOAD);
	}
	
	/**
	 * Gets the opcode associated for the type of loading/returning a desc of a field in a method
	 * @param typeDesc Desc of the field
	 * @param opcodeType TYPE_LOAD or TYPE_RETURN
	 * @return The opcode associated for the type of loading/returning a desc of a field in a method or -1 if type isn't valid
	 */
	public static int getOpcode(String typeDesc, int opcodeType) {
		if ((opcodeType != TYPE_LOAD) && (opcodeType != TYPE_RETURN))
			return -1;
		char c = typeDesc.charAt(0);
		switch (c) {
			case 'F':
				return opcodeType == TYPE_LOAD ? FLOAD : opcodeType == TYPE_RETURN ? FRETURN : -1;
			case 'D':
				return opcodeType == TYPE_LOAD ? DLOAD : opcodeType == TYPE_RETURN ? DRETURN : -1;
			case 'J':
				return opcodeType == TYPE_LOAD ? LLOAD : opcodeType == TYPE_RETURN ? LRETURN : -1;
			case 'I':
			case 'B':
			case 'Z':
			case 'S':
				return opcodeType == TYPE_LOAD ? ILOAD : opcodeType == TYPE_RETURN ? IRETURN : -1;
			default:
				return opcodeType == TYPE_LOAD ? ALOAD : opcodeType == TYPE_RETURN ? ARETURN : -1;
		}
	}
	
	/**
	 * Retrieve all {@link AbstractInsnNode}'s of a {@link MethodNode} and put them into an array.
	 * @param m MethodNode to get instructions from.
	 * @return All {@link AbstractInsnNode}'s.
	 */
	public static AbstractInsnNode[] getInstructions(MethodNode m) {
		return getInstructions(m, InstructionFilter.ACCEPT_ALL);
	}
	
	/**
	 * Retrieve filtered {@link AbstractInsnNode}'s of a {@link MethodNode} and put them into an array.
	 * @param m MethodNode to get instructions from.
	 * @param filter Filter to filter instructions.
	 * @return Filtered {@link AbstractInsnNode}'s.
	 */
	public static AbstractInsnNode[] getInstructions(MethodNode m, InstructionFilter filter) {
		ArrayList<AbstractInsnNode> insns = new ArrayList<AbstractInsnNode>();
		ListIterator<?> it = m.instructions.iterator();
		while (it.hasNext()) {
			AbstractInsnNode ain = (AbstractInsnNode) it.next();
			if (filter.accept(ain))
				insns.add(ain);
		}
		return insns.toArray(new AbstractInsnNode[insns.size()]);
	}
}
