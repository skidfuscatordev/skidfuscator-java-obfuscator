package org.mapleir.ir.code.expr.invoke;

import org.mapleir.app.service.InvocationResolver;
import org.mapleir.ir.code.Expr;
import org.mapleir.stdlib.collections.CollectionUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.mapleir.asm.MethodNode;

import java.util.HashSet;
import java.util.Set;

public class VirtualInvocationExpr extends InvocationExpr {
	public VirtualInvocationExpr(CallType callType, Expr[] args, String owner, String name, String desc) {
		super(callType, args, owner, name, desc);
		if (callType != CallType.INTERFACE && callType != CallType.SPECIAL && callType != CallType.VIRTUAL)
			throw new IllegalArgumentException(callType.toString());
	}

	@Override
	public VirtualInvocationExpr copy() {
		return new VirtualInvocationExpr(getCallType(), copyArgs(), getOwner(), getName(), getDesc());
	}

	@Override
	protected void generateCallCode(MethodVisitor visitor) {
		visitor.visitMethodInsn(resolveASMOpcode(getCallType()), getOwner(), getName(), getDesc(), isInterfaceCall());
	}
	
	public boolean isInterfaceCall() {
		return getCallType() == CallType.INTERFACE;
	}

	public boolean isSpecialCall() {
		return getCallType() == CallType.SPECIAL;
	}

	public static int resolveASMOpcode(CallType t) {
		switch (t) {
		case SPECIAL:
			return Opcodes.INVOKESPECIAL;
		case VIRTUAL:
			return Opcodes.INVOKEVIRTUAL;
		case INTERFACE:
			return Opcodes.INVOKEINTERFACE;
		default:
			throw new IllegalArgumentException(t.toString());
		}
	}

	public static CallType resolveCallType(int asmOpcode) {
		switch (asmOpcode) {
			case Opcodes.INVOKEVIRTUAL:
				return CallType.VIRTUAL;
			case Opcodes.INVOKESPECIAL:
				return CallType.SPECIAL;
			case Opcodes.INVOKEINTERFACE:
				return CallType.INTERFACE;
			case Opcodes.INVOKESTATIC:
				return CallType.STATIC;
			case Opcodes.INVOKEDYNAMIC:
				return CallType.DYNAMIC;
		default:
			throw new IllegalArgumentException(String.valueOf(asmOpcode));
		}
	}
	
	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}
	
	@Override
	public Expr[] getPrintedArgs() {
		Expr[] result = new Expr[getArgumentExprs().length - 1];
		System.arraycopy(getArgumentExprs(), 1, result, 0, getArgumentExprs().length - 1);
		return result;
	}
	
	@Override
	public Set<MethodNode> resolveTargets(InvocationResolver res) {
		if(getName().equals("<init>")) {
			assert (getCallType() == InvocationExpr.CallType.SPECIAL);
			return resolveSpecialInvocation(res, getOwner(), getDesc());
		} else {
			return resolveVirtualInvocation(res, getOwner(), getName(), getDesc());
		}
	}
	
	public static Set<MethodNode> resolveSpecialInvocation(InvocationResolver res, String owner, String desc) {
		return CollectionUtils.asCollection(HashSet::new, res.resolveVirtualInitCall(owner, desc));
	}
	
	public static Set<MethodNode> resolveVirtualInvocation(InvocationResolver res, String owner, String name, String desc) {
		// use strict mode = false if you're working with incomplete analysis.
		return res.resolveVirtualCalls(owner, name, desc, true);
	}
}
