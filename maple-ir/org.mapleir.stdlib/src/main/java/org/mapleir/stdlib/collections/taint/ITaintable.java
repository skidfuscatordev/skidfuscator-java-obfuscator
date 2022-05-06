package org.mapleir.stdlib.collections.taint;

public interface ITaintable {
	/**
	 * @return true if marked as tainted.
	 */
	boolean isTainted();
	
	/**
	 * Merge with another taintable object.
	 * The callee instance receives from the passed instance.
	 * @return whether if tainted after the merge.
	 */
	boolean union(ITaintable t);
}
