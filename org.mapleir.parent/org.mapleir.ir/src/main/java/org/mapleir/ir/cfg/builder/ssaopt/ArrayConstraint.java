package org.mapleir.ir.cfg.builder.ssaopt;

import org.mapleir.ir.code.CodeUnit;

public class ArrayConstraint implements Constraint {
	
	@Override
	public boolean fails(CodeUnit s) {
		return true;
	}
}