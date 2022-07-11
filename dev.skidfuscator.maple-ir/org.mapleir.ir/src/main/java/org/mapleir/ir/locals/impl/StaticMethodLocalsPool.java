package org.mapleir.ir.locals.impl;

import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.SSALocalsPool;

public class StaticMethodLocalsPool extends SSALocalsPool {
	public StaticMethodLocalsPool() {
		super();
	}

	@Override
	public boolean isReservedRegister(Local l) {
		return false;
	}

	@Override
	public boolean isImplicitRegister(Local l) {
		return false;
	}
}
