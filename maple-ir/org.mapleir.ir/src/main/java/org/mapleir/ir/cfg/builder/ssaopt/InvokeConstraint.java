package org.mapleir.ir.cfg.builder.ssaopt;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Opcode;

public class InvokeConstraint implements Constraint {
	@Override
	public boolean fails(CodeUnit s) {
		int op = s.getOpcode();
		return ConstraintUtil.isInvoke(op) || op == Opcode.FIELD_STORE || op == Opcode.ARRAY_STORE;
	}
}