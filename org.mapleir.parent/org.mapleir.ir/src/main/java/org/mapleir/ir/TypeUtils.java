package org.mapleir.ir;

import static org.objectweb.asm.Opcodes.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.util.Printer;

public class TypeUtils {
	
	private static Set<Type> __getIntrinsicErrors() {
		/* all extend VirtualMachineError */
		Set<Type> set = new HashSet<>();
		set.add(Type.getType(InternalError.class));
		set.add(Type.getType(OutOfMemoryError.class));
		set.add(Type.getType(StackOverflowError.class));
		set.add(Type.getType(UnknownError.class));
		return set;
	}

	public static final Set<Type> VM_ERRORS = Collections.unmodifiableSet(__getIntrinsicErrors());
	public static final Type CLASS = Type.getType(Class.class);
	public static final Type STRING_TYPE = Type.getType(String.class);
	public static final Type OBJECT_TYPE = Type.getType(Object.class);
	public static final Type CLONEABLE_TYPE = Type.getType(Cloneable.class);
	public static final Type SERIALIZABLE_TYPE = Type.getType(Serializable.class);
	public static final Type THROWABLE = Type.getType(Throwable.class);
	public static final Type NULL_TYPE = Type.getType(Null.class);
	public static final Type UNDEFINED_TYPE = Type.getType(Undefined.class);
	public static final Type UNINITIALIZED_TYPE = Type.getType(Uninitialized.class);


	// TODO: remove
	public static final Type ANY = Type.getType("L<any>;");
	
	
	public enum ArrayType {
		INT(Type.INT_TYPE, 0),
		LONG(Type.LONG_TYPE, 1),
		FLOAT(Type.FLOAT_TYPE, 2),
		DOUBLE(Type.DOUBLE_TYPE, 3),
		OBJECT(OBJECT_TYPE, 4),
		BYTE(Type.BYTE_TYPE, 5),
		CHAR(Type.CHAR_TYPE, 6),
		SHORT(Type.SHORT_TYPE, 7);
		
		private final Type type;
		private final int loadOpcode, storeOpcode;
		
		private ArrayType(Type type, int offset) {
			this.type = type;
			loadOpcode = IALOAD + offset;
			storeOpcode = IASTORE + offset;
		}
		
		public Type getType() {
			return type;
		}
		
		public int getLoadOpcode() {
			return loadOpcode;
		}
		
		public int getStoreOpcode() {
			return storeOpcode;
		}
		
		public static ArrayType resolve(int opcode) {
			if(opcode >= IALOAD && opcode <= SALOAD) {
				return values()[opcode - IALOAD];
			} else if(opcode >= IASTORE && opcode <= SASTORE) {
				return values()[opcode - IASTORE];
			} else {
				throw new UnsupportedOperationException(Printer.OPCODES[opcode]);
			}
		}
	}
	
	public static final Type[] OPCODE_TYPE_TABLE = new Type[] { Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE, OBJECT_TYPE, Type.BYTE_TYPE, Type.CHAR_TYPE,
			Type.SHORT_TYPE };

	public static boolean isPrimitive(Type type) {
		return type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE;
	}

	public static boolean isObjectRef(Type type) {
		return type.getSort() >= Type.ARRAY && type.getSort() <= Type.OBJECT;
	}

	public static Type resolveUnaryOpType(Type type) {
		if (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.INT) {
			return Type.INT_TYPE;
		} else if (type == Type.LONG_TYPE || type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE) {
			return type;
		} else {
			throw new UnsupportedOperationException("Unsupported binop types: " + type);
		}
	}

	public static Type resolveBinOpType(Type type1, Type type2) {
		if (isObjectRef(type1) || isObjectRef(type2)) {
			if (isObjectRef(type1) != isObjectRef(type2)) {
				throw new IllegalStateException("Illegal binop types: " + type1 + "    " + type2);
			}
			return OBJECT_TYPE;
		} else if (type1 == Type.DOUBLE_TYPE || type2 == Type.DOUBLE_TYPE) {
			return Type.DOUBLE_TYPE;
		} else if (type1 == Type.FLOAT_TYPE || type2 == Type.FLOAT_TYPE) {
			return Type.FLOAT_TYPE;
		} else if (type1 == Type.LONG_TYPE || type2 == Type.LONG_TYPE) {
			return Type.LONG_TYPE;
		} else if (type1.getSort() >= Type.BOOLEAN && type1.getSort() <= Type.INT && type2.getSort() >= Type.BOOLEAN && type2.getSort() <= Type.INT) {
			return Type.INT_TYPE;
		} else {
			throw new UnsupportedOperationException("Unsupported binop types: " + type1 + "    " + type2);
		}
	}

	public static Type asSimpleType(Type type) {
		if (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.INT) {
			return Type.INT_TYPE;
		} else if (type.getSort() >= Type.FLOAT && type.getSort() <= Type.DOUBLE) {
			return type;
		} else {
			return OBJECT_TYPE;
		}
	}

	public static Type getLoadType(int opcode) {
		if (opcode == ILOAD) {
			return Type.INT_TYPE;
		} else if (opcode == LLOAD) {
			return Type.LONG_TYPE;
		} else if (opcode == FLOAD) {
			return Type.FLOAT_TYPE;
		} else if (opcode == DLOAD) {
			return Type.DOUBLE_TYPE;
		} else if (opcode == ALOAD) {
			return OBJECT_TYPE;
		} else {
			throw new IllegalArgumentException(Printer.OPCODES[opcode]);
		}
	}

	public static Type getStoreType(int opcode) {
		if (opcode == ISTORE) {
			return Type.INT_TYPE;
		} else if (opcode == LSTORE) {
			return Type.LONG_TYPE;
		} else if (opcode == FSTORE) {
			return Type.FLOAT_TYPE;
		} else if (opcode == DSTORE) {
			return Type.DOUBLE_TYPE;
		} else if (opcode == ASTORE) {
			return OBJECT_TYPE;
		} else {
			throw new IllegalArgumentException(Printer.OPCODES[opcode]);
		}
	}

	public static Type getCastType(int opcode) {
		switch (opcode) {
			case I2B:
				return Type.BYTE_TYPE;
			case I2C:
				return Type.CHAR_TYPE;
			case I2S:
				return Type.SHORT_TYPE;
			case L2I:
			case F2I:
			case D2I:
				return Type.INT_TYPE;
			case I2L:
			case F2L:
			case D2L:
				return Type.LONG_TYPE;
			case I2F:
			case L2F:
			case D2F:
				return Type.FLOAT_TYPE;
			case I2D:
			case L2D:
			case F2D:
				return Type.DOUBLE_TYPE;
			default:
				throw new IllegalArgumentException(Printer.OPCODES[opcode]);
		}
	}

	public static Type getPrimitiveArrayType(int opcode) {
		switch (opcode) {
			case T_BOOLEAN:
				return Type.getType("[Z");
			case T_BYTE:
				return Type.getType("[B");
			case T_SHORT:
				return Type.getType("[S");
			case T_CHAR:
				return Type.getType("[C");
			case T_INT:
				return Type.getType("[I");
			case T_LONG:
				return Type.getType("[J");
			case T_FLOAT:
				return Type.getType("[F");
			case T_DOUBLE:
				return Type.getType("[D");
			default:
				throw new IllegalArgumentException(Printer.OPCODES[opcode]);
		}
	}
	
	public static int[] getPrimitiveCastOpcodes(Type from, Type to) {
		int sortFrom = from.getSort();
		int sortTo = to.getSort();

		switch (sortFrom) {
			case Type.BOOLEAN:
				if (sortTo == Type.BOOLEAN || sortTo == Type.BYTE || sortTo == Type.INT) {
					return new int[] {};
				}
				break;
			case Type.BYTE:
				if (sortTo == Type.BOOLEAN || sortTo == Type.BYTE || sortTo == Type.INT) {
					return new int[] {};
				} else if (sortTo == Type.SHORT) {
					return new int[] { I2S };
				} else if (sortTo == Type.CHAR) {
					return new int[] { I2C };
				} else if (sortTo == Type.LONG) {
					return new int[] { I2L };
				} else if (sortTo == Type.FLOAT) {
					return new int[] { I2F };
				} else if (sortTo == Type.DOUBLE) {
					return new int[] { I2D };
				}
				break;
			case Type.SHORT:
				if (sortTo == Type.BOOLEAN || sortTo == Type.SHORT || sortTo == Type.INT) {
					return new int[] {};
				} else if (sortTo == Type.BYTE) {
					return new int[] { I2B };
				} else if (sortTo == Type.CHAR) {
					return new int[] { I2C };
				} else if (sortTo == Type.LONG) {
					return new int[] { I2L };
				} else if (sortTo == Type.FLOAT) {
					return new int[] { I2F };
				} else if (sortTo == Type.DOUBLE) {
					return new int[] { I2D };
				}
				break;
			case Type.CHAR:
				if (sortTo == Type.BOOLEAN || sortTo == Type.CHAR || sortTo == Type.INT) {
					return new int[] {};
				} else if (sortTo == Type.BYTE) {
					return new int[] { I2B };
				} else if (sortTo == Type.SHORT) {
					return new int[] { I2S };
				} else if (sortTo == Type.LONG) {
					return new int[] { I2L };
				} else if (sortTo == Type.FLOAT) {
					return new int[] { I2F };
				} else if (sortTo == Type.DOUBLE) {
					return new int[] { I2D };
				}
				break;
			case Type.INT:
				if (sortTo == Type.BOOLEAN || sortTo == Type.INT) {
					return new int[] {};
				} else if (sortTo == Type.BYTE) {
					return new int[] { I2B };
				} else if (sortTo == Type.SHORT) {
					return new int[] { I2S };
				} else if (sortTo == Type.CHAR) {
					return new int[] { I2C };
				} else if (sortTo == Type.LONG) {
					return new int[] { I2L };
				} else if (sortTo == Type.FLOAT) {
					return new int[] { I2F };
				} else if (sortTo == Type.DOUBLE) {
					return new int[] { I2D };
				}
				break;
			case Type.LONG:
				if (sortTo == Type.BYTE) {
					return new int[] { L2I, I2B };
				} else if (sortTo == Type.SHORT) {
					return new int[] { L2I, I2S };
				} else if (sortTo == Type.CHAR) {
					return new int[] { L2I, I2C };
				} else if (sortTo == Type.INT || sortTo == Type.BOOLEAN) {
					return new int[] { L2I };
				} else if (sortTo == Type.LONG) {
					return new int[] {};
				} else if (sortTo == Type.FLOAT) {
					return new int[] { L2F };
				} else if (sortTo == Type.DOUBLE) {
					return new int[] { L2D };
				}
				break;
			case Type.FLOAT:
				if (sortTo == Type.BYTE) {
					return new int[] { F2I, I2B };
				} else if (sortTo == Type.SHORT) {
					return new int[] { F2I, I2S };
				} else if (sortTo == Type.CHAR) {
					return new int[] { F2I, I2C };
				} else if (sortTo == Type.INT || sortTo == Type.BOOLEAN) {
					return new int[] { F2I };
				} else if (sortTo == Type.LONG) {
					return new int[] { F2L };
				} else if (sortTo == Type.FLOAT) {
					return new int[] {};
				} else if (sortTo == Type.DOUBLE) {
					return new int[] { F2D };
				}
				break;
			case Type.DOUBLE:
				if (sortTo == Type.BYTE) {
					return new int[] { D2I, I2B };
				} else if (sortTo == Type.SHORT) {
					return new int[] { D2I, I2S };
				} else if (sortTo == Type.CHAR) {
					return new int[] { D2I, I2C };
				} else if (sortTo == Type.INT || sortTo == Type.BOOLEAN) {
					return new int[] { D2I };
				} else if (sortTo == Type.LONG) {
					return new int[] { D2L };
				} else if (sortTo == Type.FLOAT) {
					return new int[] { D2F };
				} else if (sortTo == Type.DOUBLE) {
					return new int[] {};
				}
				break;
		}

		throw new IllegalArgumentException("illegal cast from " + from.toString() + " to " + to.toString());
	}

	public static int getPrimitiveArrayOpcode(Type type) {
		switch (type.getElementType().getSort()) {
			case Type.BOOLEAN:
				return Opcodes.T_BOOLEAN;
			case Type.BYTE:
				return Opcodes.T_BYTE;
			case Type.SHORT:
				return Opcodes.T_SHORT;
			case Type.CHAR:
				return Opcodes.T_CHAR;
			case Type.INT:
				return Opcodes.T_INT;
			case Type.LONG:
				return Opcodes.T_LONG;
			case Type.FLOAT:
				return Opcodes.T_FLOAT;
			case Type.DOUBLE:
				return Opcodes.T_DOUBLE;
			default:
				throw new RuntimeException("WT");
		}
	}

	public static int getAddOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return IADD;
		} else if (type == Type.LONG_TYPE) {
			return LADD;
		} else if (type == Type.FLOAT_TYPE) {
			return FADD;
		} else if (type == Type.DOUBLE_TYPE) {
			return DADD;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getSubtractOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return ISUB;
		} else if (type == Type.LONG_TYPE) {
			return LSUB;
		} else if (type == Type.FLOAT_TYPE) {
			return FSUB;
		} else if (type == Type.DOUBLE_TYPE) {
			return DSUB;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getMultiplyOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return IMUL;
		} else if (type == Type.LONG_TYPE) {
			return LMUL;
		} else if (type == Type.FLOAT_TYPE) {
			return FMUL;
		} else if (type == Type.DOUBLE_TYPE) {
			return DMUL;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getDivideOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return IDIV;
		} else if (type == Type.LONG_TYPE) {
			return LDIV;
		} else if (type == Type.FLOAT_TYPE) {
			return FDIV;
		} else if (type == Type.DOUBLE_TYPE) {
			return DDIV;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getRemainderOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return IREM;
		} else if (type == Type.LONG_TYPE) {
			return LREM;
		} else if (type == Type.FLOAT_TYPE) {
			return FREM;
		} else if (type == Type.DOUBLE_TYPE) {
			return DREM;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getBitAndOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return IAND;
		} else if (type == Type.LONG_TYPE) {
			return LAND;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getBitOrOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return IOR;
		} else if (type == Type.LONG_TYPE) {
			return LOR;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getBitXorOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return IXOR;
		} else if (type == Type.LONG_TYPE) {
			return LXOR;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getBitShiftLeftOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return ISHL;
		} else if (type == Type.LONG_TYPE) {
			return LSHL;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int bitShiftRightOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return ISHR;
		} else if (type == Type.LONG_TYPE) {
			return LSHR;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getBitShiftRightUnsignedOpcode(Type type) {
		if (type == Type.INT_TYPE) {
			return IUSHR;
		} else if (type == Type.LONG_TYPE) {
			return LUSHR;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	public static int getNegateOpcode(Type type) {
		if (type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE) {
			return Opcodes.INEG;
		} else if (type == Type.LONG_TYPE) {
			return Opcodes.LNEG;
		} else if (type == Type.FLOAT_TYPE) {
			return Opcodes.FNEG;
		} else if (type == Type.DOUBLE_TYPE) {
			return Opcodes.DNEG;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}
	
	public static int getVariableLoadOpcode(Type type) {
		if (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.INT) {
			return Opcodes.ILOAD;
		} else if (type == Type.LONG_TYPE) {
			return Opcodes.LLOAD;
		} else if (type == Type.FLOAT_TYPE) {
			return Opcodes.FLOAD;
		} else if (type == Type.DOUBLE_TYPE) {
			return Opcodes.DLOAD;
		} else if (type.getSort() >= Type.ARRAY && type.getSort() <= Type.OBJECT) {
			return Opcodes.ALOAD;
		} else {
			return getVariableLoadOpcode(asSimpleType(type));
//			throw new IllegalArgumentException(type.toString());
		}
	}
	
	public static int getVariableStoreOpcode(Type type) {
		if (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.INT) {
			return Opcodes.ISTORE;
		} else if (type == Type.LONG_TYPE) {
			return Opcodes.LSTORE;
		} else if (type == Type.FLOAT_TYPE) {
			return Opcodes.FSTORE;
		} else if (type == Type.DOUBLE_TYPE) {
			return Opcodes.DSTORE;
		} else if (type.getSort() >= Type.ARRAY && type.getSort() <= Type.OBJECT) {
			return Opcodes.ASTORE;
		} else {
			return getVariableStoreOpcode(asSimpleType(type));
			// throw new IllegalArgumentException(type.toString());
		}
	}
	
	public static int getDupOpcode(Type type) {
		if (type.getSize() == 1) {
			return Opcodes.DUP;
		} else if (type.getSize() == 2) {
			return Opcodes.DUP2;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}
	
	public static int getDupXOpcode(Type dType, Type bType) {
		if (dType.getSize() == 1 && bType.getSize() == 1) {
			return Opcodes.DUP_X1;
		} else if (dType.getSize() == 1 && bType.getSize() == 2) {
			return Opcodes.DUP_X2;
		} else if (dType.getSize() == 2 && bType.getSize() == 1) {
			return Opcodes.DUP2_X1;
		} else if (dType.getSize() == 2 && bType.getSize() == 2) {
			return Opcodes.DUP2_X2;
		} else {
			throw new IllegalArgumentException(dType.toString() + " " + bType.toString());
		}
	}
	
	public static int getPopOpcode(Type type) {
		if (type.getSize() == 1) {
			return Opcodes.POP;
		} else if (type.getSize() == 2) {
			return Opcodes.POP2;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}
	
	public static int getReturnOpcode(Type type) {
		if (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.INT) {
			return Opcodes.IRETURN;
		} else if (type == Type.LONG_TYPE) {
			return Opcodes.LRETURN;
		} else if (type == Type.FLOAT_TYPE) {
			return Opcodes.FRETURN;
		} else if (type == Type.DOUBLE_TYPE) {
			return Opcodes.DRETURN;
		} else if (type.getSort() >= Type.ARRAY && type.getSort() <= Type.OBJECT) {
			return Opcodes.ARETURN;
		} else {
			throw new IllegalArgumentException(type.toString());
		}
	}

	private static final Map<Class<?>, Type> unboxTable = new HashMap<>();
	
	static {
		// unboxTable.put(Boolean.class, Type.BOOLEAN_TYPE);
		unboxTable.put(Character.class, Type.CHAR_TYPE);
		unboxTable.put(Byte.class, Type.BYTE_TYPE);
		unboxTable.put(Short.class, Type.SHORT_TYPE);
		unboxTable.put(Integer.class, Type.INT_TYPE);
		unboxTable.put(Float.class, Type.FLOAT_TYPE);
		unboxTable.put(Double.class, Type.DOUBLE_TYPE);
		unboxTable.put(Long.class, Type.LONG_TYPE);
		unboxTable.put(Null.class, NULL_TYPE);
		unboxTable.put(Undefined.class, UNDEFINED_TYPE);
		unboxTable.put(String.class, STRING_TYPE);
	}
	
	public static Type unboxType(Object cst) {
		Class<?> c = cst.getClass();
		
		Type t = unboxTable.get(c);
		if(t == null) {
			if(c == Type.class) {
				Type type = (Type) cst;
				if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
					return Type.getType("Ljava/lang/Class;");
				} else if (type.getSort() == Type.METHOD) {
					return Type.getType("Ljava/lang/invoke/MethodType;");
				} else {
					throw new RuntimeException("Invalid type: " + cst);
				}
			} else {
				throw new UnsupportedOperationException(c.getName() + " : " + unboxTable.containsKey(c));
			}
		} else {
			return t;
		}
	}
	
	public static Number rebox(Number cst, Type t) {
		if(t == Type.BYTE_TYPE) {
			return cst.byteValue();
		}  else if(t == Type.SHORT_TYPE) {
			return cst.shortValue();
		} else if(t == Type.INT_TYPE) {
			return cst.intValue();
		} else if(t == Type.LONG_TYPE) {
			return cst.longValue();
		} else if(t == Type.FLOAT_TYPE) {
			return cst.floatValue();
		} else if(t == Type.DOUBLE_TYPE) {
			return cst.doubleValue();
		} else {
			throw new UnsupportedOperationException(String.format("%s (%s) to %s", cst, cst.getClass(), t));
		}
	}
	
	/**
	 * @see <a
	 *      href=https://docs.oracle.com/javase/specs/jls/se7/html/jls-10.html>JLS
	 *      ref</a>
	 * @param dst The LHS type.
	 * @return whether an array can be assigned to the given
	 *         type due to the implicit hierarchy policy
	 *         specified in the JLS.
	 */
	public static boolean isImplicitArraySuperType(Type dst) {
		return dst.equals(TypeUtils.OBJECT_TYPE) || dst.equals(TypeUtils.CLONEABLE_TYPE)
				|| dst.equals(TypeUtils.SERIALIZABLE_TYPE);
	}

	public static boolean canStoreClass(ApplicationClassSource source, Type src, Type dst) {
		if(src.getSort() == Type.ARRAY || dst.getSort() == Type.ARRAY) {
			throw new UnsupportedOperationException(String.format("%s vs %s", src, dst));
		}
		return canStoreClass(source.getClassTree(), source.findClassNode(src.getInternalName()), source.findClassNode(dst.getInternalName()));
	}
	
	/* If child is an interface that is not a subinterface of
	 * parent, this method will return false even though some
	 * objects implementing the child interface may also
	 * implement the parent interface. */
	public static boolean canStoreClass(ClassTree tree, ClassNode src, ClassNode dst) {
		if(src == dst) {
			throw new IllegalStateException();
		}
		
		if(src == dst) {
			return true;
		} else if(dst.getName().equals("java/lang/Object")) {
			return true;
		}
		
		return tree.getAllParents(src).contains(dst);
	}
	
	public static boolean isRefLikeType(Type t) {
		return t.getSort() == Type.ARRAY || t.getSort() == Type.OBJECT;
	}
}
