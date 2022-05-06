package org.mapleir.ir.locals.impl;

import org.mapleir.ir.locals.Local;

public class VersionedLocal extends Local {

	private int subscript;
	
	public VersionedLocal(int index, int subscript) {
		super(index);
		if (index > INDEX_MASK || subscript > SUBSCRIPT_MASK)
			throw new IllegalArgumentException("Index/subscript overflow; hashCode collision possible " + index + " " + subscript);
		this.subscript = subscript;
	}
	
	public VersionedLocal(int index, int subscript, boolean stack) {
		super(index, stack);
		if (index > INDEX_MASK || subscript > SUBSCRIPT_MASK)
			throw new IllegalArgumentException("Index/subscript overflow; hashCode collision possible " + index + " " + subscript);
		this.subscript = subscript;
	}
	
	public int getSubscript() {
		return subscript;
	}
	
	@Override
	public int getCodeIndex() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return super.toString() + "_" + subscript;
	}

	private static final int SUBSCRIPT_MASK = (1 << 24) - 1; // 24 bits for subscript
	private static final int INDEX_MASK = (1 << 11) - 1; // 11 bits for index
	
	@Override
	public int hashCode() {
		return ((isStack() ? 0 : 1) << 31) | ((getIndex() & INDEX_MASK) << 11) | (getSubscript() & SUBSCRIPT_MASK);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		else if (o instanceof VersionedLocal) {
			VersionedLocal other = (VersionedLocal) o;
			return isStack() == other.isStack() && getIndex() == other.getIndex() && getSubscript() == other.getSubscript();
		} else
			return false;
	}
	
	@Override
	public int compareTo(Local o) {
		if(!(o instanceof VersionedLocal)) {
			throw new UnsupportedOperationException(this + " vs " + o.toString());
		}

		int comp = super.compareTo(o);
		
		VersionedLocal v = (VersionedLocal) o;
		if(subscript == 0 && v.subscript != 0) {
			return -1;
		} else if(subscript != 0 && v.subscript == 0) {
			return 1;
		}
		
		if(comp == 0) {
			comp = Integer.compare(subscript, v.subscript);
		}
		return comp;
	}
}
