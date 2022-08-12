package org.mapleir.ir.cfg.builder.ssaopt;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.stmt.FieldStoreStmt;

public class FieldConstraint implements Constraint {
	private final String key;
	
	public FieldConstraint(FieldLoadExpr le) {
		key = le.getName() + "." + le.getDesc();
	}
	
	@Override
	public boolean fails(CodeUnit s) {
		int op = s.getOpcode();
		if(op == Opcode.FIELD_STORE) {
			FieldStoreStmt store = (FieldStoreStmt) s;
			String key2 = store.getName() + "." + store.getDesc();
			if(key2.equals(key)) {
				return true;
			}
		} else if(ConstraintUtil.isInvoke(op)) {
			return true;
		}
		return false;
	}
}