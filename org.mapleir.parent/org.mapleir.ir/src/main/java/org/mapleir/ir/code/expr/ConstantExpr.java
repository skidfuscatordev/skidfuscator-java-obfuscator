package org.mapleir.ir.code.expr;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

@Getter @Setter
public class ConstantExpr extends Expr {

	// TODO: add validation
	private Object constant;
	private Type type;
		
	public ConstantExpr(Object cst) {
		this(cst, computeType(cst), true);
	}
	
	public ConstantExpr(Object cst, Type type, boolean check) {
		super(CONST_LOAD);
		
		if (cst instanceof ConstantExpr) {
			throw new IllegalArgumentException("nice try cowboy");
		}
		if(type == Type.BOOLEAN_TYPE) {
			throw new RuntimeException("TODO");
		}
		
		Type ctype = null;
		if(check) {
			if (!(cst == null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY))) {
				if (!type.equals(ctype = computeType(cst)) && !isAcceptableSupertype(ctype, type))
					throw new IllegalStateException(cst + ", " + type + ", " + ctype);
			}
		} else {
			if(cst instanceof Number && !TypeUtils.unboxType(cst).equals(type)) {
				if(ctype == null) {
					ctype = computeType(cst);
				}
				cst = TypeUtils.rebox((Number) cst, ctype);
				// throw new RuntimeException(String.format("rebox: %s (%s) to %s (%s)", cst, cst.getClass(), type, TypeUtils.rebox((Number)cst, computeType(cst)).getClass()));
			}
		}


		this.constant = cst;
		this.type = type;
	}
	
	public ConstantExpr(Object cst, Type type) {
		this(cst, type, true);
	}

	@Override
	public ConstantExpr copy() {
		return new ConstantExpr(constant, type, false);
	}

	private static boolean isAcceptableSupertype(Type child, Type parent) {
		if (child.equals(parent))
			return true;
		if (child.equals(Type.BYTE_TYPE))
			return parent.equals(Type.INT_TYPE) || parent.equals(Type.SHORT_TYPE) || parent.equals(Type.LONG_TYPE) || parent.equals(Type.CHAR_TYPE);
		if (child.equals(Type.SHORT_TYPE))
			return parent.equals(Type.INT_TYPE) || parent.equals(Type.LONG_TYPE) || parent.equals(Type.CHAR_TYPE);
		if (child.equals(Type.CHAR_TYPE))
			return parent.equals(Type.INT_TYPE) || parent.equals(Type.LONG_TYPE);
		if (child.equals(Type.INT_TYPE))
			return parent.equals(Type.LONG_TYPE);
		if (child.equals(Type.FLOAT_TYPE))
			return parent.equals(Type.DOUBLE_TYPE);
		return false;
	}
	
	public static Type computeType(Object cst) {
		if (cst == null) {
			return Type.getType("Ljava/lang/Object;");
		} else if (cst instanceof Integer) {
			int val = ((Integer) cst).intValue();
			if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
				return Type.BYTE_TYPE;
			} else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
				return Type.SHORT_TYPE;
			} else {
				return Type.INT_TYPE;
			}
//			return Type.INT_TYPE;
		} else if (cst instanceof Long) {
			return Type.LONG_TYPE;
		} else if (cst instanceof Float) {
			return Type.FLOAT_TYPE;
		} else if (cst instanceof Double) {
			return Type.DOUBLE_TYPE;
		} else if (cst instanceof String) {
			return Type.getType("Ljava/lang/String;");
		} else if (cst instanceof Type) {
			Type type = (Type) cst;
			if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
				return Type.getType("Ljava/lang/Class;");
			} else if (type.getSort() == Type.METHOD) {
				return Type.getType("Ljava/lang/invoke/MethodType;");
			} else {
				throw new RuntimeException("Invalid type: " + cst);
			}
		} else if (cst instanceof Handle) {
			return Type.getType("Ljava/lang/invoke/MethodHandle;");
		} else if (cst instanceof Boolean) {
			return Type.BOOLEAN_TYPE;
		} else if(cst instanceof Byte) {
			return Type.BYTE_TYPE;
		} else if (cst instanceof Character) {
			return Type.CHAR_TYPE;
		} else if(cst instanceof Short) {
			return Type.SHORT_TYPE;
		} else {
			throw new RuntimeException("Invalid type: " + cst);
		}
	}
	
	@Override
	public Type getType() {
		return type;
	}

	@Override
	public void onChildUpdated(int ptr) {
		raiseChildOutOfBounds(ptr);
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		if (constant == null) {
			printer.print("nullconst");
		} else if (constant instanceof Integer || constant instanceof Byte || constant instanceof Short) {
			printer.print(constant + "");
		} else if (constant instanceof Long) {
			printer.print(constant + "L");
		} else if (constant instanceof Float) {
			printer.print(constant + "F");
		} else if (constant instanceof Double) {
			printer.print(constant + "D");
		} else if (constant instanceof String) {
			printer.print("\"" + constant + "\"");
		} else if (constant instanceof Type) {
			Type type = (Type) constant;
			if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
				printer.print(type.getClassName() + ".class");
			} else if (type.getSort() == Type.METHOD) {
				printer.print("methodTypeOf(" + type + ")");
			} else {
				throw new RuntimeException("WT");
			}
		} else if (constant instanceof Handle) {
			printer.print("handleOf(" + constant + ")");
		} else if (constant instanceof Boolean) {
			// synthetic values
			printer.print(constant + "");
		} else if (constant instanceof Character) {
			// TODO , normal character printing
			printer.print('\'');
			printer.print((char) constant);
			printer.print('\'');
		} else {
			throw new IllegalStateException(constant + " : " + constant.getClass());
		}
	}

	public static void packInt(MethodVisitor visitor, int value) {
		if (value >= -1 && value <= 5) {
			visitor.visitInsn(Opcodes.ICONST_M1 + (value + 1));
		} else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
			visitor.visitIntInsn(Opcodes.BIPUSH, value);
		} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
			visitor.visitIntInsn(Opcodes.SIPUSH, value);
		} else {
			visitor.visitLdcInsn(value);
		}
	}
	
	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		if (constant == null) {
			visitor.visitInsn(Opcodes.ACONST_NULL);
		} else if (constant instanceof Integer || constant instanceof Byte || constant instanceof Short) {
			Number n = (Number) constant;
			packInt(visitor, n.intValue());
		} else if (constant instanceof Long) {
			long value = (long) constant;
			if (value == 0L || value == 1L) {
				visitor.visitInsn(value == 0L ? Opcodes.LCONST_0 : Opcodes.LCONST_1);
			} else {
				visitor.visitLdcInsn(value);
			}
		} else if (constant instanceof Float) {
			float value = (float) constant;
			if (value == 0F || value == 1F || value == 2F) {
				visitor.visitInsn(Opcodes.FCONST_0 + (int) value);
			} else {
				visitor.visitLdcInsn(value);
			}
		} else if (constant instanceof Double) {
			double value = (double) constant;
			if (value == 0D || value == 1D) {
				visitor.visitInsn(value == 0 ? Opcodes.DCONST_0 : Opcodes.DCONST_1);
			} else {
				visitor.visitLdcInsn(value);
			}
		} else if (constant instanceof String || constant instanceof Handle || constant instanceof Type) {
			visitor.visitLdcInsn(constant);
		}
		// synthethic values
		else if (constant instanceof Boolean) {
			boolean value = (boolean) constant;
			visitor.visitInsn(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
		} else if (constant instanceof Character) {
			char value = (char) constant;
			packInt(visitor, value);
		} else {
			throw new IllegalStateException(constant + " : " + constant.getClass());
		}
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof ConstantExpr) {
			ConstantExpr c = (ConstantExpr) s;
			if(constant == null) {
				if(c.constant == null) {
					return true;
				} else {
					return false;
				}
			} else {
				if(c.constant == null) {
					return false;
				} else {
					return constant.equals(c.constant);
				}
			}
		}
		return false;
	}
}