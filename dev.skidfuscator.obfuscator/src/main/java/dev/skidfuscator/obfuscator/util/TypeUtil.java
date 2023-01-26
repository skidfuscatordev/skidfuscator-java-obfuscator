package dev.skidfuscator.obfuscator.util;

import dev.skidfuscator.obfuscator.Skidfuscator;
import lombok.experimental.UtilityClass;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.TypeUtils;
import org.objectweb.asm.Type;

import java.util.Arrays;

/**
 * Utilities for ASM's {@link Type} class <i>(And some additional descriptor cases)</i>
 *
 * @author Matt from Recaf
 */

@UtilityClass
public class TypeUtil {
	private final Type[] PRIMITIVES = new Type[]{
		Type.VOID_TYPE,
		Type.BOOLEAN_TYPE,
		Type.BYTE_TYPE,
		Type.CHAR_TYPE,
		Type.SHORT_TYPE,
		Type.INT_TYPE,
		Type.FLOAT_TYPE,
		Type.DOUBLE_TYPE,
		Type.LONG_TYPE
	};

	/**
	 * Cosntant for object type.
	 */
	public final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
	public final Type NULL_TYPE = TypeUtils.NULL_TYPE;
	public final Type UNDEFINED_TYPE = TypeUtils.UNDEFINED_TYPE;

	public final Type UNINITIALIZED_THIS = TypeUtils.UNINITIALIZED_TYPE;
	public final Type STRING_TYPE = Type.getObjectType("java/lang/String");
	public final Type THROWABLE_TYPE = Type.getType(Throwable.class);
	public final Type CLASS_TYPE = Type.getType("Ljava/lang/Class;");
	public final Type BYTE_ARRAY_TYPE = Type.getType(byte[].class);


	/**
	 * private sort denoting an object type, such as "com/Example" versus the
	 * standard "Lcom/Example;".
	 */
	private final int INTERNAL = 12;

	/**
	 * @param desc
	 *            Type to check.
	 * @return Type denotes a primitive type.
	 */
	public boolean isPrimitiveDesc(String desc) {
		if(desc.length() != 1) {
			return false;
		}
		switch(desc.charAt(0)) {
			case 'Z':
			case 'C':
			case 'B':
			case 'S':
			case 'I':
			case 'F':
			case 'J':
			case 'D':
				return true;
			default:
				return false;
		}
	}

	/**
	 * @param arg
	 * 		Operand value of a NEWARRAY instruction.
	 *
	 * @return Array element type.
	 */
	public Type newArrayArgToType(int arg) {
		switch(arg) {
			case 4: return Type.BOOLEAN_TYPE;
			case 5: return Type.CHAR_TYPE;
			case 6: return Type.FLOAT_TYPE;
			case 7: return Type.DOUBLE_TYPE;
			case 8: return Type.BYTE_TYPE;
			case 9: return Type.SHORT_TYPE;
			case 10: return Type.INT_TYPE;
			case 11: return Type.LONG_TYPE;
			default: break;
		}
		throw new IllegalArgumentException("Unexpected NEWARRAY arg: " + arg);
	}

	/**
	 * @param type
	 * 		Array element type.
	 *
	 * @return Operand value for a NEWARRAY instruction.
	 */
	public int typeToNewArrayArg(Type type) {
		switch(type.getDescriptor().charAt(0)) {
			case 'Z': return 4;
			case 'C': return 5;
			case 'F': return 6;
			case 'D': return 7;
			case 'B': return 8;
			case 'S': return 9;
			case 'I': return 10;
			case 'J': return 11;
			default: break;
		}
		throw new IllegalArgumentException("Unexpected NEWARRAY type: " + type.getDescriptor());
	}

	/**
	 * @param desc
	 *            Text to check.
	 * @return {@code true} when the descriptor is in method format, "(Ltype/args;)Lreturn;"
	 */
	public boolean isMethodDesc(String desc) {
		// This assumes a lot, but hey, it serves our purposes.
		return desc.charAt(0) == '(';
	}

	/**
	 * @param desc
	 *            Text to check.
	 * @return {@code true} when the descriptor is in standard format, "Lcom/Example;".
	 */
	public boolean isFieldDesc(String desc) {
		return desc.length() > 2 && desc.charAt(0) == 'L' && desc.charAt(desc.length() - 1) == ';';
	}

	/**
	 * @param desc
	 *            Text to check.
	 * @return Type is object/internal format of "com/Example".
	 */
	public boolean isInternal(String desc) {
		return !isMethodDesc(desc) && !isFieldDesc(desc);
	}

	/**
	 * Convert a Type sort to a string representation.
	 *
	 * @param sort
	 * 		Type sort value.
	 *
	 * @return Sort string value.
	 */
	public String sortToString(int sort) {
		switch(sort) {
			case Type.VOID:
				return "VOID";
			case Type.BOOLEAN:
				return "BOOLEAN";
			case Type.CHAR:
				return "CHAR";
			case Type.BYTE:
				return "BYTE";
			case Type.SHORT:
				return "SHORT";
			case Type.INT:
				return "INT";
			case Type.FLOAT:
				return "FLOAT";
			case Type.LONG:
				return "LONG";
			case Type.DOUBLE:
				return "DOUBLE";
			case Type.ARRAY:
				return "ARRAY";
			case Type.OBJECT:
				return "OBJECT";
			case Type.METHOD:
				return "METHOD";
			case INTERNAL:
				return "INTERNAL";
			default:
				return "UNKNOWN";
		}
	}

	/**
	 * @param sort
	 * 		Type sort<i>(kind)</i>
	 *
	 * @return Size of type.
	 */
	public int sortToSize(int sort) {
		switch(sort) {
			case Type.LONG:
			case Type.DOUBLE:
				return 2;
			default:
				return 1;
		}
	}

	/**
	 * @param sort
	 * 		Type sort<i>(kind)</i>
	 *
	 * @return Size of type.
	 */
	public int size(Type sort) {
		switch(sort.getSort()) {
			case Type.LONG:
			case Type.DOUBLE:
				return 2;
			default:
				return 1;
		}
	}

	/**
	 * @param type
	 * 		Some array type.
	 *
	 * @return Array depth.
	 */
	public int getArrayDepth(Type type) {
		if (type.getSort() == Type.ARRAY)
			return type.getDimensions();
		return 0;
	}

	/**
	 * @param desc
	 * 		Some class name.
	 *
	 * @return {@code true} if it matches the class name of a primitive type.
	 */
	public boolean isPrimitiveClassName(String desc) {
		for (Type prim : PRIMITIVES)
			if (prim.getClassName().equals(desc))
				return true;
		return false;
	}

	/**
	 * @param desc
	 * 		Must be a primitive class name. See {@link #isPrimitiveClassName(String)}.
	 *
	 * @return Internal name.
	 */
	public String classToPrimitive(String desc) {
		for (Type prim : PRIMITIVES)
			if (prim.getClassName().equals(desc))
				return prim.getInternalName();
		throw new IllegalArgumentException("Descriptor was not a primitive class name!");
	}

	/**
	 * @param node
	 * 		Must be a skid classnode or any sort of class extending MapleIR's class
	 *
	 * @return Internal type
	 */
	public Type getType(final ClassNode node) {
		return Type.getType("L" + node.getName());
	}

	public static Type mergeTypes(final Skidfuscator skidfuscator, final Type head, final Type newest) {
		/*
		 * If the parent type is null (undefined),
		 * then just skip. We don't need this.
		 */
		if (head == null/* || newest != null && head.equals(UNDEFINED_TYPE)*/)
			return newest;

		/*
		 * If the newest type is null (undefined),
		 * then just return the head.
		 */
		if (newest == null/* || newest.equals(UNDEFINED_TYPE)*/) {
			return head;
		}

		/*
		 * EEEEE
		 */
		if (head.equals(TypeUtil.UNDEFINED_TYPE) || newest.equals(TypeUtil.UNDEFINED_TYPE))
			return TypeUtil.UNDEFINED_TYPE;

		/*
		 * If the proposed type is identical, we
		 * can move onto the next parent as the
		 * local is coherent.
		 */
		if (newest.equals(head))
			return newest;

		/*
		 * Here's a conflict. The proposed type
		 * and parent type are different. Yikes.
		 *
		 * 1) If    they are both references
		 *    Then  get common supertype
		 *
		 * 2) If    they are anything else
		 *    Then  push exception
		 *
		 * todo: array support
		 */
		if (head.getSort() == Type.OBJECT && newest.getSort() == Type.OBJECT
				&& !head.equals(TypeUtil.NULL_TYPE) && !newest.equals(TypeUtil.NULL_TYPE)
				&& !head.equals(TypeUtil.UNDEFINED_TYPE) && !newest.equals(TypeUtil.UNDEFINED_TYPE)) {
			final ClassNode selfClassNode = skidfuscator
					.getClassSource()
					.findClassNode(head.getInternalName());
			final ClassNode otherClassNode = skidfuscator
					.getClassSource()
					.findClassNode(newest.getInternalName());

			final ClassNode commonClassNode = skidfuscator.getClassSource()
					.getClassTree()
					.getCommonAncestor(Arrays.asList(selfClassNode, otherClassNode))
					.iterator()
					.next();

			Skidfuscator.LOGGER.warn("/!\\ Merged " + selfClassNode.getName() + " and " + otherClassNode.getName() + " to type " + commonClassNode.getName());
			return Type.getType("L" + commonClassNode.getName() + ";");
		} /*else if ((head.equals(TypeUtil.NULL_TYPE) && newest.getSort() == Type.OBJECT)
				|| (head.getSort() == Type.OBJECT && newest.equals(TypeUtil.NULL_TYPE))) {
			return TypeUtil.OBJECT_TYPE;
		}*/ else if (true /* debug */) {
			return Type.VOID_TYPE;
		}

		/* kekw this suckz */
		throw new IllegalStateException(
				"Incompatible merge types: \n" +
						"Head: " + head + "\n" +
						"Newest: "+ newest + "\n"
		);
	}
}