package org.mapleir.ir.code.expr.invoke;

import org.mapleir.app.service.InvocationResolver;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.collections.CollectionUtils;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.mapleir.asm.MethodNode;

import java.util.HashSet;
import java.util.Set;

public class InitialisedObjectExpr extends Invocation {

	private String owner;
	private String desc;
	private Expr[] args;
	
	public InitialisedObjectExpr(String owner, String desc, Expr[] args) {
		super(INIT_OBJ);
		this.owner = owner;
		this.desc = desc;
		this.args = args;
		for (int i = 0; i < args.length; i++) {
			writeAt(args[i], i);
		}
	}

	@Override
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public void setArgumentExprs(Expr[] args) {
		this.args = args;

		for (int i = 0; i < args.length; i++) {
			writeAt(args[i], i);
		}
	}

	@Override
	public Type getType() {
		return Type.getType("L" + owner + ";");
	}
	
	@Override
	public Precedence getPrecedence0() {
		return Precedence.METHOD_INVOCATION;
	}
	
	@Override
	public void onChildUpdated(int ptr) {
		Expr argument = read(ptr);
		if (ptr < 0 || (ptr) >= args.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		
		args[ptr] = argument;
	}

	@Override
	public InitialisedObjectExpr copy() {
		Expr[] exprs = new Expr[args.length];
		for(int i=0; i < args.length; i++) {
			exprs[i] = args[i].copy();
		}
		return new InitialisedObjectExpr(owner, desc, exprs);
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		printer.print("new ");
		printer.print(owner);
		printer.print('(');
		for (int i = 0; i < args.length; i++) {
			boolean needsComma = (i + 1) < args.length;
			args[i].toString(printer);
			if (needsComma)
				printer.print(", ");
		}
		printer.print(')');
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		Type[] argTypes = Type.getArgumentTypes(desc);
		if (argTypes.length < args.length) {
			Type[] bck = argTypes;
			argTypes = new Type[bck.length + 1];
			System.arraycopy(bck, 0, argTypes, 1, bck.length);
			argTypes[0] = Type.getType("L" + owner + ";");
		}
		
		visitor.visitTypeInsn(Opcodes.NEW, owner);
		visitor.visitInsn(Opcodes.DUP);
		for (int i = 0; i < args.length; i++) {
			args[i].toCode(visitor, assembler);
			if (TypeUtils.isPrimitive(args[i].getType())) {
				int[] cast = TypeUtils.getPrimitiveCastOpcodes(args[i].getType(), argTypes[i]);
				for (int a = 0; a < cast.length; a++)
					visitor.visitInsn(cast[a]);
			}
		}
		visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", desc, false);		
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof InitialisedObjectExpr) {
			InitialisedObjectExpr o = (InitialisedObjectExpr) s;
			if(!owner.equals(o.owner) || !desc.equals(o.desc)) {
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
	public boolean isStatic() {
		return false;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public Expr getPhysicalReceiver() {
		return null;
	}

	@Override
	public Expr[] getArgumentExprs() {
		return args;
	}

	@Override
	public Expr[] getParameterExprs() {
		return args;
	}

	@Override
	public void setName(String name) {
		throw new IllegalStateException("Cannot rename name");
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

	@Override
	public String getName() {
		return "<init>";
	}

	@Override
	public Set<MethodNode> resolveTargets(InvocationResolver res) {
		return CollectionUtils.asCollection(HashSet::new, res.resolveVirtualInitCall(getOwner(), getDesc()));
	}
}
