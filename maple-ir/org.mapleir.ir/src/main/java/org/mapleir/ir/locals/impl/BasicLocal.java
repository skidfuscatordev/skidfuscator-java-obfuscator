package org.mapleir.ir.locals.impl;

import org.mapleir.ir.locals.Local;

public class BasicLocal extends Local {

	public BasicLocal(int index) {
		this(index, false);
	}
	
	public BasicLocal(int index, boolean stack) {
		super(index, stack);
	}
}
