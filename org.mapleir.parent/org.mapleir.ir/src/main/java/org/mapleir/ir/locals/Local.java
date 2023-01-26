package org.mapleir.ir.locals;

import org.objectweb.asm.Type;

public abstract class Local implements Comparable<Local> {

	private Type type;
	private final boolean stack;
	private int index;
	private boolean tempLocal;

	/**
	 * Bitfield for boolean state information of this unit.
	 */
	protected int flags;
	
	public Local(int index) {
		this(index, false);
	}
	
	public Local(int index, boolean stack) {
		this.index = index;
		this.stack = stack;
		if (index < 0)
			throw new IllegalArgumentException("Index underflow; hashCode collision possible " + index);
	}
	
	public boolean isStack() {
		return stack;
	}

	public int getIndex() {
		return index;
	}
	
	public int getCodeIndex() {
//		return stack ? getBase() + index : index;
		return index;
	}

	private static final boolean DEBUG_PRINT = false;
	@Override
	public String toString() {
		if (DEBUG_PRINT)
			return (stack ? "S" : "L") + /*"var" +*/ index;
		return (stack ? "s" : "l") + "var" + index;
	}

	public boolean isTempLocal() {
		return tempLocal;
	}

	public void setTempLocal(boolean temp) {
		if (temp && !isStack())
			throw new UnsupportedOperationException("Local variables cannot be stored in a temp lvar");
		tempLocal = temp;
	}

	public boolean isStoredInLocal() {
		return !isStack() || isTempLocal();
	}

	public void setIndex(int index) {
		this.index = index;
	}
	public void setType(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public void setFlag(int flag, boolean val) {
		if(val) {
			flags |= flag;
		} else {
			flags ^= flag;
		}
	}

	public boolean isFlagSet(int f) {
		return ((flags & f) != 0);
	}

	@Override
	public int compareTo(Local o) {
		if(stack && !o.stack) {
			return -1;
		} else if(!stack && o.stack) {
			return 1;
		}
		return Integer.compare(index, o.index);
	}

	@Override
	public int hashCode() {
		return ((stack ? 0 : 1) << 31) | index;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		else if (o instanceof Local) {
			Local other = (Local) o;
			return stack == other.stack && index == other.index;
		} else
			return false;
	}
}
