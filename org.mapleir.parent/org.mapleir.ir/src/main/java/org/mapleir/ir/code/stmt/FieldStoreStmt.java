package org.mapleir.ir.code.stmt;

import lombok.Getter;
import lombok.Setter;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class FieldStoreStmt extends Stmt implements IUsesJavaDesc {

	// TODO: Add validation
	private Expr instanceExpression;
	private Expr valueExpression;
	private String owner;
	private String name;
	private String desc;
	private boolean isStatic;

	public FieldStoreStmt(Expr instanceExpression, Expr valueExpression, String owner, String name, String desc, boolean isStatic) {
		super(FIELD_STORE);
		this.setOwner(owner);
		this.setName(name);
		this.setDesc(desc);
		this.setStatic(isStatic);
		this.setInstanceExpression(instanceExpression);
		this.setValueExpression(valueExpression);
	}

	public void setInstanceExpression(Expr instanceExpression) {
		if (this.instanceExpression != null) {
			this.instanceExpression.unlink();
		}

		this.instanceExpression = instanceExpression;
		if (instanceExpression != null)
			instanceExpression.setParent(this);
	}

	public void setValueExpression(Expr valueExpression) {
		if (this.valueExpression != null) {
			this.valueExpression.unlink();
		}

		this.valueExpression = valueExpression;
		this.valueExpression.setParent(this);
	}

	@Deprecated
	@Override
	public void onChildUpdated(int ptr) {
		throw new UnsupportedOperationException("Deprecated");
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
			this.setValueExpression(newest);
			return;
		} else if (instanceExpression == previous) {
			this.setInstanceExpression(newest);
			return;
		}

		super.overwrite(previous, newest);
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

	@Override
	public List<CodeUnit> children() {
		final List<CodeUnit> self = new ArrayList<>();

		if (instanceExpression != null)
			self.add(instanceExpression);
		self.add(valueExpression);

		return Collections.unmodifiableList(self);
	}
}
