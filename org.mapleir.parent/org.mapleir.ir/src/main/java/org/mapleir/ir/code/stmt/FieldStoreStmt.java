package org.mapleir.ir.code.stmt;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Expr.Precedence;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.*;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FieldStoreStmt extends Stmt implements IUsesJavaDesc {

	private Expr instanceExpression;
	private Expr valueExpression;
	private String owner;
	private String name;
	private String desc;
	private boolean isStatic;

	public FieldStoreStmt(Expr instanceExpression, Expr valueExpression, String owner, String name, String desc, boolean isStatic) {
		super(FIELD_STORE);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.isStatic = isStatic;
		
		writeAt(instanceExpression, 0);
		writeAt(valueExpression, instanceExpression == null ? 0 : 1);
	}

	public boolean isStatic() {
		return isStatic;
	}
	
	public Expr getInstanceExpression() {
		return instanceExpression;
	}

	public void setInstanceExpression(Expr instanceExpression) {
		if (this.instanceExpression == null && instanceExpression != null) {
			this.instanceExpression = instanceExpression;
			writeAt(valueExpression, 1);
			writeAt(this.instanceExpression, 0);
		} else if (this.instanceExpression != null && instanceExpression == null) {
			this.instanceExpression = instanceExpression;
			writeAt(valueExpression, 0);
			writeAt(null, 1);
		} else {
			this.instanceExpression = instanceExpression;
			writeAt(this.instanceExpression, 0);
		}
	}

	public Expr getValueExpression() {
		return valueExpression;
	}

	public void setValueExpression(Expr valueExpression) {
		writeAt(valueExpression, instanceExpression == null ? 0 : 1);
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public void onChildUpdated(int ptr) {
		if(isStatic) {
			if(ptr == 0) {
				valueExpression = read(0);
			} else {
				raiseChildOutOfBounds(ptr);
			}
		} else {
			if(ptr == 0) {
				instanceExpression = read(0);
			} else if(ptr == 1) {
				valueExpression = read(1);
			} else {
				raiseChildOutOfBounds(ptr);
			}
		}
	}

	@Override
	public void toString(TabbedStringWriter printer) {
		if (instanceExpression != null) {
			int selfPriority = Precedence.MEMBER_ACCESS.ordinal();
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
		printer.print(" = ");
		valueExpression.toString(printer);
		printer.print(';');
	}

	@Override
	public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
		if (instanceExpression != null)
			instanceExpression.toCode(visitor, assembler);
		valueExpression.toCode(visitor, assembler);
		if (TypeUtils.isPrimitive(Type.getType(desc))) {
			int[] cast = TypeUtils.getPrimitiveCastOpcodes(valueExpression.getType(), Type.getType(desc));
			for (int i = 0; i < cast.length; i++)
				visitor.visitInsn(cast[i]);
		}
		visitor.visitFieldInsn(instanceExpression != null ? Opcodes.PUTFIELD : Opcodes.PUTSTATIC, owner, name, desc);		
	}

	@Override
	public boolean canChangeFlow() {
		return false;
	}

	@Override
	public void overwrite(Expr previous, Expr newest) {
		if (valueExpression == previous) {
			setValueExpression(newest);
			valueExpression = newest;
		} else if (instanceExpression == previous) {
			setInstanceExpression(newest);
			instanceExpression = newest;
		}

		//super.overwrite(previous, newest);
	}

	@Override
	public FieldStoreStmt copy() {
		return new FieldStoreStmt(instanceExpression == null ? null : instanceExpression.copy(), valueExpression.copy(), owner, name, desc, isStatic);
	}

	@Override
	public boolean equivalent(CodeUnit s) {
		if(s instanceof FieldStoreStmt) {
			FieldStoreStmt store = (FieldStoreStmt) s;
			return isStatic == store.isStatic && owner.equals(store.owner) && name.equals(store.name) && desc.equals(store.desc) &&
					instanceExpression.equivalent(store.instanceExpression) && valueExpression.equivalent(store.valueExpression);
		}
		return false;
	}

	@Override
	public JavaDesc.DescType getDescType() {
		return JavaDesc.DescType.FIELD;
	}

	@Override
	public JavaDescUse.UseType getDataUseType() {
		return JavaDescUse.UseType.WRITE;
	}

	@Override
	public JavaDesc getDataUseLocation() {
		return getBlock().getGraph().getJavaDesc();
	}
}
