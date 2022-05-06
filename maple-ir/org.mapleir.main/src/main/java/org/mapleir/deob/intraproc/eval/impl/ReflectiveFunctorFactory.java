package org.mapleir.deob.intraproc.eval.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.mapleir.deob.intraproc.eval.EvaluationFactory;
import org.mapleir.deob.intraproc.eval.EvaluationFunctor;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ComparisonExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.mapleir.asm.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ReflectiveFunctorFactory implements EvaluationFactory {
	private final BridgeDefiningClassLoader classLoader;
	private final Map<String, EvaluationFunctor<?>> cache;
	
	public ReflectiveFunctorFactory() {
		classLoader = new BridgeDefiningClassLoader();
		cache = new HashMap<>();
	}
	
	@SuppressWarnings("unchecked")
	private <T> EvaluationFunctor<T> _get(String name) {
		return (EvaluationFunctor<T>) cache.get(name);
	}
	
	@Override
	public EvaluationFunctor<Boolean> branch(Type lt, Type rt, ConditionalJumpStmt.ComparisonType type) {
		Type opType = TypeUtils.resolveBinOpType(lt, rt);
		String name = lt.getClassName() + type.name() + rt.getClassName() + "OPTYPE" + opType.getClassName() + "RETbool";

		String desc = "(" + lt.getDescriptor() + rt.getDescriptor() + ")Z";

		if(cache.containsKey(name)) {
			return _get(name);
		}

		MethodNode m = makeBase(name, desc);
		{
			InsnList insns = new InsnList();
			
			insns.add(new VarInsnNode(TypeUtils.getVariableLoadOpcode(lt), 0));
			cast(insns, lt, opType);
			insns.add(new VarInsnNode(TypeUtils.getVariableLoadOpcode(rt), lt.getSize()));
			cast(insns, rt, opType);
			

			LabelNode trueSuccessor = new LabelNode();
			
			if (opType == Type.INT_TYPE) {
				insns.add(new JumpInsnNode(Opcodes.IF_ICMPEQ + type.ordinal(), trueSuccessor));
			} else if (opType == Type.LONG_TYPE) {
				insns.add(new InsnNode(Opcodes.LCMP));
				insns.add(new JumpInsnNode(Opcodes.IFEQ + type.ordinal(), trueSuccessor));
			} else if (opType == Type.FLOAT_TYPE) {
				insns.add(new InsnNode((type == ConditionalJumpStmt.ComparisonType.LT || type == ConditionalJumpStmt.ComparisonType.LE) ? Opcodes.FCMPL : Opcodes.FCMPG));
				insns.add(new JumpInsnNode(Opcodes.IFEQ + type.ordinal(), trueSuccessor));
			} else if (opType == Type.DOUBLE_TYPE) {
				insns.add(new InsnNode((type == ConditionalJumpStmt.ComparisonType.LT || type == ConditionalJumpStmt.ComparisonType.LE) ? Opcodes.DCMPL : Opcodes.DCMPG));
				insns.add(new JumpInsnNode(Opcodes.IFEQ + type.ordinal(), trueSuccessor));
			} else {
				throw new IllegalArgumentException(opType.toString());
			}
			
			branchReturn(insns, trueSuccessor);
			
			m.node.instructions = insns;
		}
		
		return buildBridge(m);
	}
	
	@Override
	public EvaluationFunctor<Number> compare(Type lt, Type rt, ComparisonExpr.ValueComparisonType type) {
		String name = lt.getClassName() + type.name() + rt.getClassName() + "RETint";
		if(cache.containsKey(name)) {
			return _get(name);
		}
		
		String desc = "(" + lt.getDescriptor() + rt.getDescriptor() + ")I";
		MethodNode m = makeBase(name, desc);
		{
			Type opType = TypeUtils.resolveBinOpType(lt, rt);
			
			InsnList insns = new InsnList();
			insns.add(new VarInsnNode(TypeUtils.getVariableLoadOpcode(lt), 0));
			cast(insns, lt, opType);
			insns.add(new VarInsnNode(TypeUtils.getVariableLoadOpcode(rt), lt.getSize()));
			cast(insns, rt, opType);
		
			int op;
			if (opType == Type.DOUBLE_TYPE) {
				op = type == ComparisonExpr.ValueComparisonType.GT ? Opcodes.DCMPG : Opcodes.DCMPL;
			} else if (opType == Type.FLOAT_TYPE) {
				op = type == ComparisonExpr.ValueComparisonType.GT ? Opcodes.FCMPG : Opcodes.FCMPL;
			} else if (opType == Type.LONG_TYPE) {
				op = Opcodes.LCMP;
			} else {
				throw new IllegalArgumentException();
			}
			insns.add(new InsnNode(op));
			insns.add(new InsnNode(Opcodes.IRETURN));
			
			m.node.instructions = insns;
		}
		
		return buildBridge(m);
	}
	
	@Override
	public EvaluationFunctor<Number> cast(Type from, Type to) {
		String name = "CASTFROM" + from.getClassName() + "TO" + to.getClassName();
		
		if(cache.containsKey(name)) {
			return _get(name);
		}

		String desc = ("(" + from.getDescriptor() + ")" + to.getDescriptor());
		MethodNode m = makeBase(name, desc);
		
		InsnList insns = new InsnList();
		{
			insns.add(new VarInsnNode(TypeUtils.getVariableLoadOpcode(from), 0));
			cast(insns, from, to);
			insns.add(new InsnNode(TypeUtils.getReturnOpcode(to)));
			m.node.instructions = insns;
		}
		
		return buildBridge(m);
	}
	
	@Override
	public EvaluationFunctor<Number> negate(Type t) {
		String name = "NEG" + t.getClassName();
		
		if(cache.containsKey(name)) {
			return _get(name);
		}
		
		String desc = ("(" + t.getDescriptor() + ")" + t.getDescriptor());
		MethodNode m = makeBase(name, desc);
		
		InsnList insns = new InsnList();
		{
			insns.add(new VarInsnNode(TypeUtils.getVariableLoadOpcode(t), 0));
			insns.add(new InsnNode(TypeUtils.getNegateOpcode(t)));
			insns.add(new InsnNode(TypeUtils.getReturnOpcode(t)));
			m.node.instructions = insns;
		}
		
		return buildBridge(m);
	}
	
	@Override
	public EvaluationFunctor<Number> arithmetic(Type t1, Type t2, Type rt, ArithmeticExpr.Operator op) {
		String name = t1.getClassName() + op.name() + t2.getClassName() + "RET" + rt.getClassName();
		
		if(cache.containsKey(name)) {
			return _get(name);
		}
		
		String desc = ("(" + t1.getDescriptor() + t2.getDescriptor() + ")" + rt.getDescriptor());
		MethodNode m = makeBase(name, desc);
		
		InsnList insns = new InsnList();
		{
			Type leftType = null;
			Type rightType = null;
			if (op == ArithmeticExpr.Operator.SHL || op == ArithmeticExpr.Operator.SHR || op == ArithmeticExpr.Operator.USHR) {
				leftType = rt;
				rightType = Type.INT_TYPE;
			} else {
				leftType = rightType = rt;
			}
			
			insns.add(new VarInsnNode(TypeUtils.getVariableLoadOpcode(t1), 0));
			cast(insns, t1, leftType);

			insns.add(new VarInsnNode(TypeUtils.getVariableLoadOpcode(t2), t1.getSize() /*D,J=2, else 1*/));
			cast(insns, t2, rightType);
			
			int opcode;
			switch (op) {
				case ADD:
					opcode = TypeUtils.getAddOpcode(rt);
					break;
				case SUB:
					opcode = TypeUtils.getSubtractOpcode(rt);
					break;
				case MUL:
					opcode = TypeUtils.getMultiplyOpcode(rt);
					break;
				case DIV:
					opcode = TypeUtils.getDivideOpcode(rt);
					break;
				case REM:
					opcode = TypeUtils.getRemainderOpcode(rt);
					break;
				case SHL:
					opcode = TypeUtils.getBitShiftLeftOpcode(rt);
					break;
				case SHR:
					opcode = TypeUtils.bitShiftRightOpcode(rt);
					break;
				case USHR:
					opcode = TypeUtils.getBitShiftRightUnsignedOpcode(rt);
					break;
				case OR:
					opcode = TypeUtils.getBitOrOpcode(rt);
					break;
				case AND:
					opcode = TypeUtils.getBitAndOpcode(rt);
					break;
				case XOR:
					opcode = TypeUtils.getBitXorOpcode(rt);
					break;
				default:
					throw new RuntimeException();
			}
			
			insns.add(new InsnNode(opcode));
			insns.add(new InsnNode(TypeUtils.getReturnOpcode(rt)));
			
			m.node.instructions = insns;
		}
		
		return buildBridge(m);
	}

	private void cast(InsnList insns, Type from, Type to) {
		int[] cast = TypeUtils.getPrimitiveCastOpcodes(from, to);
		for (int i = 0; i < cast.length; i++) {
			insns.add(new InsnNode(cast[i]));
		}
	}
	
	private void branchReturn(InsnList insns, LabelNode trueSuccessor) {
		// return false
		insns.add(new InsnNode(Opcodes.ICONST_0));
		insns.add(new InsnNode(Opcodes.IRETURN));
		insns.add(trueSuccessor);
		// return true
		insns.add(new InsnNode(Opcodes.ICONST_1));
		insns.add(new InsnNode(Opcodes.IRETURN));
	}
	
	private MethodNode makeBase(String name, String desc) {
		ClassNode owner = new ClassNode();
		owner.node.version = Opcodes.V1_7;
		owner.node.name = name;
		owner.node.superName = "java/lang/Object";
		owner.node.access = Opcodes.ACC_PUBLIC;
		
		MethodNode m = new MethodNode(new org.objectweb.asm.tree.MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "eval", desc, null, null), owner);
		owner.addMethod(m);
		return m;
	}
	
	private <T> EvaluationFunctor<T> buildBridge(MethodNode m) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		ClassNode owner = m.owner;
		owner.node.accept(cw);
		
		byte[] bytes = cw.toByteArray();
		Class<?> clazz = classLoader.make(owner.getName(), bytes);
		
		for(Method method : clazz.getDeclaredMethods()) {
			if(method.getName().equals("eval")) {
				EvaluationFunctor<T> f = new ReflectiveFunctor<>(method);
				
				cache.put(owner.getName(), f);
				return f;
			}
		}
		
		throw new UnsupportedOperationException();
	}
	
	private static class BridgeDefiningClassLoader extends ClassLoader {
		public Class<?> make(String name, byte[] bytes) {
			return defineClass(name.replace("/", "."), bytes, 0, bytes.length);
		}
	}
}
