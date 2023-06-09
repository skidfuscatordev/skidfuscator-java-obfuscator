package org.mapleir.ir.code.expr.invoke;

import org.apache.log4j.Logger;
import org.mapleir.app.service.InvocationResolver;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.mapleir.asm.MethodNode;

import java.util.HashSet;
import java.util.Set;

public class DynamicInvocationExpr extends InvocationExpr {

	/**
	 * The function factory, e.g. the binder.
	 * ex: metafactory
	 */
	private Handle bootstrapMethod;
	
	/**
	 * Mandatory args to the bootstrapper specifying the lambda to bind and the "decomposed" function type
	 * Ex: {Test, lambda$func$0, (ILjava/lang/Integer;)V}
	 */
	private Object[] bootstrapArgs;
	
	/**
	 * Name of the bound method.
	 * Ex: accept (for Consumer), run (for Runnable)
	 */
	private String boundName;
	
	public DynamicInvocationExpr(Handle bootstrapMethod, Object[] bootstrapArgs, String bootstrapDesc, Expr[] args, String boundName) {
		super(CallType.DYNAMIC, args, bootstrapMethod.getOwner(), bootstrapMethod.getName(), bootstrapDesc);
		
		this.bootstrapMethod = bootstrapMethod;
		this.bootstrapArgs = bootstrapArgs;
		this.boundName = boundName;
		//assert(Type.getArgumentTypes(bootstrapDesc).length == args.length) : "You fucked up"; // I hope this tells me when this fucks up, because this is not a matter of if, but when.
		
		for(int i = 0; i < args.length; i++) {
			writeAt(args[i], i);
		}
	}

	public Handle getBootstrapMethod() {
		return bootstrapMethod;
	}
	
	public Object[] getBootstrapArgs() {
		return bootstrapArgs;
	}

	public void setBootstrapArgs(Object[] bootstrapArgs) {
		this.bootstrapArgs = bootstrapArgs;
	}

	public String getBoundName() {
		return boundName;
	}

	/**
	 * The descriptor of the bootstrapper method (given the 3 bootstrapArgs parameters implicitly passed).
	 * This is key, since it tells you how it affects the stack!
	 * Ex: (LTest;I)Ljava/util/function/Consumer;
	 */
	public String getBootstrapDesc() {
		return getDesc();
	}
	
	/**
	 * The value of the bound args.
	 * Ex: {lvar0, 0}
	 */
	public Expr[] getBoundArgs() {
		return getArgumentExprs();
	}
	
	public Type getProvidedFuncType() {
		return Type.getMethodType(getBootstrapDesc()).getReturnType();
	}

	@Override
	public DynamicInvocationExpr copy() {
		Handle bsmHandleCopy = new Handle(bootstrapMethod.getTag(), bootstrapMethod.getOwner(), bootstrapMethod.getName(), bootstrapMethod.getDesc());
		Object[] bsmArgsCopy = new Object[bootstrapArgs.length];
		System.arraycopy(bootstrapArgs, 0, bsmArgsCopy, 0, bsmArgsCopy.length);
		return new DynamicInvocationExpr(bsmHandleCopy, bsmArgsCopy, getBootstrapDesc(), copyArgs(), boundName);
	}
	
	@Override
	public Type getType() {
		return getProvidedFuncType();
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("dynamic_invoke<");
		printer.print(getProvidedFuncType().getClassName());
		printer.print(">(");
		super.toString(printer);
		printer.print(")");
	}
	
	@Override
	public Expr[] getPrintedArgs() {
		Expr[] result = new Expr[bootstrapArgs.length + getArgumentExprs().length];
		for (int i = 0; i < bootstrapArgs.length; i++)
			result[i] = new ConstantExpr(bootstrapArgs[i]); // this is such a hack!
		System.arraycopy(getArgumentExprs(), 0, result, bootstrapArgs.length, getArgumentExprs().length);
		return result;
	}

	@Override
	protected void generateCallCode(MethodVisitor visitor) {
		visitor.visitInvokeDynamicInsn(boundName, getBootstrapDesc(), bootstrapMethod, bootstrapArgs);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if (!super.equivalent(s))
			return false;

		DynamicInvocationExpr o = (DynamicInvocationExpr) s;
		if (!boundName.equals(o.boundName))
			return false;
		if (!bootstrapMethod.equals(o.bootstrapMethod))
			return false;
		if (bootstrapArgs.length != o.bootstrapArgs.length)
			return false;
		for (int i = 0; i < bootstrapArgs.length; i++)
			if (!bootstrapArgs[i].equals(o.bootstrapArgs[i]))
				return false;
		return true;
	}
	
	@Override
	public Set<MethodNode> resolveTargets(InvocationResolver res) {
		// this is probably like 99% of all the invokedynamics
		if (bootstrapMethod.getOwner().equals("java/lang/invoke/LambdaMetafactory") 
				&& bootstrapMethod.getName().equals("metafactory")) {
			assert (bootstrapArgs.length == 3 && bootstrapArgs[1] instanceof Handle);
			Handle boundFunc = (Handle) bootstrapArgs[1];
			switch (boundFunc.getTag()) {
			case Opcodes.H_INVOKESTATIC:
				return StaticInvocationExpr.resolveStaticCall(res, boundFunc.getOwner(), boundFunc.getName(), boundFunc.getDesc());
			case Opcodes.H_INVOKESPECIAL:
				if (!boundFunc.getName().equals("<init>")) {
					Logger.getLogger(this.getClass()).warn("Lambda function invocation of type H_INVOKESPECIAL is linking against " + boundFunc.getName());
				}
				//assert(boundFunc.getName().equals("<init>"));
				return VirtualInvocationExpr.resolveSpecialInvocation(res, boundFunc.getOwner(), boundFunc.getDesc());
			case Opcodes.H_INVOKEINTERFACE:
			case Opcodes.H_INVOKEVIRTUAL:
				return VirtualInvocationExpr.resolveVirtualInvocation(res, boundFunc.getOwner(), boundFunc.getOwner(), boundFunc.getDesc());
			default:
				throw new IllegalArgumentException("Unexpected metafactory bootstrap tag?? " + boundFunc.getTag());
			}
		}
		return new HashSet<>();
	}
}
