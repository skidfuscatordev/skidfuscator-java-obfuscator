package org.mapleir.ir.code.expr;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.*;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FieldLoadExpr extends Expr implements IUsesJavaDesc {

	private Expr instanceExpression;
	private String owner;
	private String name;
	private String desc;
	private boolean isStatic;

	public FieldLoadExpr(Expr instanceExpression, String owner, String name, String desc, boolean isStatic) {
		super(FIELD_LOAD);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.isStatic = isStatic;
		setInstanceExpression(instanceExpression);
	}
	
	public boolean isStatic() {
		return isStatic;
	}

	public Expr getInstanceExpression() {
		return instanceExpression;
	}

	public void setInstanceExpression(Expr instanceExpression) {
		writeAt(instanceExpression, 0);
	}

	@Override
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public FieldLoadExpr copy() {
		return new FieldLoadExpr(instanceExpression == null ? null : instanceExpression.copy(), owner, name, desc, isStatic);
	}

	@Override
	public Type getType() {
		return Type.getType(desc);
	}

	@Override
	public void onChildUpdated(int ptr) {
		if(isStatic) {
			raiseChildOutOfBounds(ptr);
		} else {
			if(ptr == 0) {
				instanceExpression = read(0);
			} else {
				raiseChildOutOfBounds(ptr);
			}
		}
	}
	
	@Override
	public Precedence getPrecedence0() {
		return instanceExpression != null ? Precedence.MEMBER_ACCESS : Precedence.NORMAL;
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		if (instanceExpression != null) {
			int selfPriority = getPrecedence();
			int basePriority = instanceExpression.getPrecedence();
			if (basePriority > selfPriority)
				printer.print('(');
			instanceExpression.toString(printer);
			if (basePriority > selfPriority)
				printer.print(')');
		} else
			printer.print(owner.replace('/', '.'));
		printer.print('.');
		printer.print(name);
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		if (instanceExpression != null) {
			instanceExpression.toCode(visitor, assembler);
		}
		visitor.visitFieldInsn(instanceExpression != null ? Opcodes.GETFIELD : Opcodes.GETSTATIC, owner, name, desc);		
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (instanceExpression == previous) {
			instanceExpression = newest;
		}

		super.overwrite(previous, newest);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof FieldLoadExpr) {
			FieldLoadExpr load = (FieldLoadExpr) s;
			if(instanceExpression != null && load.instanceExpression == null) {
				return false;
			} else if(instanceExpression == null && load.instanceExpression != null) {
				return false;
			} else if(instanceExpression != null && load.instanceExpression != null) {
				if(!instanceExpression.equivalent(load.instanceExpression)) {
					return false;
				}
			}
			return isStatic == load.isStatic && name.equals(load.name) && desc.equals(load.desc) && owner.equals(load.owner);
		}
		return false;
	}

	@Override
	public JavaDesc.DescType getDescType() {
		return JavaDesc.DescType.FIELD;
	}

	@Override
	public JavaDescUse.UseType getDataUseType() {
		return JavaDescUse.UseType.READ;
	}

	@Override
	public JavaDesc getDataUseLocation() {
		return getBlock().getGraph().getJavaDesc();
	}
}
