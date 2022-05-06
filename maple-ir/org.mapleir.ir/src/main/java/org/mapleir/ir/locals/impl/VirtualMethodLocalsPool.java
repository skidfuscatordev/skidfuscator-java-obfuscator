package org.mapleir.ir.locals.impl;

import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;

public class VirtualMethodLocalsPool extends LocalsPool {

	public VirtualMethodLocalsPool() {
		super();
	}

	@Override
	public boolean isReservedRegister(Local l) {
		return isSelfReceiverRegister(l);
	}

	@Override
	public boolean isImplicitRegister(Local l) {
		return isSelfReceiverRegister(l);
	}
	
	public static boolean isSelfReceiverRegister(Local l) {
		boolean isSelfReceiver = !l.isStack() && l.getIndex() == 0;
		
		if (l instanceof VersionedLocal) {
			VersionedLocal vl = (VersionedLocal) l;
			return isSelfReceiver && vl.getSubscript() == 0;
		} else {
			return isSelfReceiver;
		}
	}
}
