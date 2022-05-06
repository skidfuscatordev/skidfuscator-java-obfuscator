package org.mapleir.ir.cfg.builder.ssaopt;

import org.mapleir.ir.code.CodeUnit;

public interface Constraint {
	boolean fails(CodeUnit s);
}