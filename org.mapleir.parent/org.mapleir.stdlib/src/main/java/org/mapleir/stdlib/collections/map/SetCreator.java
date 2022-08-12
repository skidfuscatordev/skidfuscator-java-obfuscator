package org.mapleir.stdlib.collections.map;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Bibl (don't ban me pls)
 */
public class SetCreator<T> implements ValueCreator<Set<T>> {
	private static final SetCreator<?> INSTANCE = new SetCreator<>();
	
	@SuppressWarnings("unchecked")
	public static <T> SetCreator<T> getInstance() {
		return (SetCreator<T>) INSTANCE;
	}
	
	@Override 
	public Set<T> create() {
		return new HashSet<>();
	}
}
