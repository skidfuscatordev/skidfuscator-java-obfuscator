package org.mapleir.ir.code.expr.invoke;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.*;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class InvocationExpr extends Invocation implements IUsesJavaDesc {
	public enum CallType {
		STATIC(Opcodes.INVOKESTATIC),
		SPECIAL(Opcodes.INVOKESPECIAL),
		VIRTUAL(Opcodes.INVOKEVIRTUAL),
		INTERFACE(Opcodes.INVOKEINTERFACE),
		DYNAMIC(Opcodes.INVOKEDYNAMIC);

		private final int opcode;

		CallType(int opcode) {
			this.opcode = opcode;
		}

		public int getOpcode() {
			return opcode;
		}
	}
	
	private CallType callType;
	private Expr[] args;
	private String owner;
	private String name;
	private String desc;

	public InvocationExpr(CallType callType, Expr[] args, String owner, String name, String desc) {
		super(INVOKE);
		
		this.callType = callType;
		this.args = args;
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		
		for (int i = 0; i < args.length; i++) {
			writeAt(args[i], i);
		}
	}

	public final CallType getCallType() {
		return callType;
	}

	public void setCallType(CallType callType) {
		this.callType = callType;
	}

	@Override
	public final String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public final String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public final String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public Expr[] copyArgs() {
		Expr[] arguments = new Expr[args.length];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = args[i].copy();
		}
		return arguments;
	}

	/**
	 * The implementer MUST call copyArgs when passing args!!!
	 * @return a copy of this invocationExpr
	 */
	@Override
	abstract public InvocationExpr copy();
	
	@Override
	public Type getType() {
		return Type.getReturnType(desc);
	}

	@Override
	public void onChildUpdated(int index) {
		Expr argument = read(index);
		if (index < 0 || (index) >= args.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		
		args[index] = argument;
		writeAt(argument, index);
	}
	
	@Override
	public Precedence getPrecedence0() {
		return Precedence.METHOD_INVOCATION;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		if (!isStatic()) {
			int memberAccessPriority = Precedence.MEMBER_ACCESS.ordinal();
			Expr instanceExpression = args[0];
			int instancePriority = instanceExpression.getPrecedence();
			if (instancePriority > memberAccessPriority) {
				printer.print('(');
			}
			instanceExpression.toString(printer);
			if (instancePriority > memberAccessPriority) {
				printer.print(')');
			}
		} else {
			printer.print(owner.replace('/', '.'));
		}
		printer.print('.');
		printer.print(name);
		printer.print('(');
		Expr[] printedArgs = getPrintedArgs();
		for (int i = 0; i < printedArgs.length; i++) {
			if (printedArgs[i] == null) {
				printer.print("null");
			} else {
				printedArgs[i].toString(printer);
			}
			if ((i + 1) < printedArgs.length) {
				printer.print(", ");
			}
		}
		printer.print(')');
	}
	
	public abstract Expr[] getPrintedArgs();
	
	protected abstract void generateCallCode(MethodVisitor visitor); 

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		Type[] argTypes = Type.getArgumentTypes(desc);
		if (!isStatic()) {
			Type[] bck = argTypes;
			argTypes = new Type[bck.length + 1];
			System.arraycopy(bck, 0, argTypes, 1, bck.length);
			argTypes[0] = Type.getType("L" + owner + ";");
		}

		try {
			for (int i = 0; i < args.length; i++) {
				args[i].toCode(visitor, assembler);
				if (callType != CallType.DYNAMIC && TypeUtils.isPrimitive(args[i].getType())) {
					assert i < args.length : String.format(
							"Failed on class %s to match args (%s)",
							this.getClass().getName(),
							Arrays.toString(args)
					);

					assert i < argTypes.length : String.format(
							"Failed on class %s to match argTypes (%s)\ndesc: %s\n\n  \\-->%s",
							this.getClass().getName(),
							Arrays.toString(argTypes),
							desc,
							Arrays.stream(args).map(CodeUnit::toString).collect(Collectors.joining("\n  \\-->"))
					);
					int[] cast = TypeUtils.getPrimitiveCastOpcodes(args[i].getType(), argTypes[i]);
					for (int a = 0; a < cast.length; a++) {
						visitor.visitInsn(cast[a]);
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalStateException(String.format("Failed to write class %s to match argTypes (%s)\ndesc: %s\n\n  \\-->%s",
					this.getClass().getName(),
					Arrays.toString(argTypes),
					desc,
					Arrays.stream(args).map(CodeUnit::toString).collect(Collectors.joining("\n  \\-->"))),
					e
			);
		}

		generateCallCode(visitor);
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof InvocationExpr) {
			InvocationExpr o = (InvocationExpr) s;
			if(callType != o.callType || !name.equals(o.name) || !owner.equals(o.owner) || !desc.equals(o.desc)) {
				return false;
			}
			if(args.length != o.args.length) {
				return false;
			}
			for(int i=0; i < args.length; i++) {
				if(!args[i].equivalent(o.args[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public Set<Expr> _enumerate() {
		Set<Expr> set = new HashSet<>();

		for (Expr arg : args) {
			set.add(arg);

			if (arg == null)
				continue;

			set.addAll(arg._enumerate());
		}
		return set;
	}

	@Override
	public Expr getPhysicalReceiver() {
		if(isStatic()) {
			return null;
		} else {
			return args[0];
		}
	}

	@Override
	public Expr[] getParameterExprs() {
		int i = isStatic() ? 0 : 1;
		Expr[] exprs = new Expr[args.length - i];
		System.arraycopy(args, i, exprs, 0, exprs.length);
		return exprs;
	}

	@Override
	public final Expr[] getArgumentExprs() {
		return args;
	}

	public void setArgumentExprs(Expr[] args) {
		this.args = args;

		for (int i = 0; i < args.length; i++) {
			writeAt(args[i], i);
		}
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		int index = -1;
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] == previous) {
				index = i;
				break;
			}
		}

		if (index == -1)
			throw new IllegalStateException("Parent has already disassociated with child");
		this.args[index] = newest;

		super.overwrite(previous, newest);
	}

	// @Override
	// public Set<MethodNode> resolveTargets(InvocationResolver res) {		
	// 	String owner = getOwner();
	// 	String name = getName();
	// 	String desc = getDesc();
	//
	// 	if(isStatic()) {
	// 		return CollectionUtils.asCollection(HashSet::new, res.resolveStaticCall(owner, name, desc));
	// 	} else {
	// 		if(name.equals("<init>")) {
	// 			return CollectionUtils.asCollection(HashSet::new, res.resolveVirtualInitCall(owner, desc));
	// 		} else {
	// 			return res.resolveVirtualCalls(owner, name, desc, true);
	// 		}
	// 	}
	// }

	@Override
	public JavaDesc.DescType getDescType() {
		return JavaDesc.DescType.METHOD;
	}

	@Override
	public JavaDescUse.UseType getDataUseType() {
		return JavaDescUse.UseType.CALL;
	}

	@Override
	public JavaDesc getDataUseLocation() {
		return getBlock().getGraph().getJavaDesc();
	}
}
