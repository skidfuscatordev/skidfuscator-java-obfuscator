package org.mapleir.ir.code;

import java.util.Set;

public abstract class Stmt extends CodeUnit {

	public Stmt(int opcode) {
		super(opcode);
		
		flags |= FLAG_STMT;
	}
	
	@Override
	public abstract Stmt copy();
	
	public Iterable<CodeUnit> enumerateWithSelf() {
//		Set<CodeUnit> set = new HashSet<>(_enumerate());
		@SuppressWarnings("unchecked")
		Set<CodeUnit> set = (Set<CodeUnit>) (Set<?>) _enumerate();
		set.add(this);
		return set;
	}
}