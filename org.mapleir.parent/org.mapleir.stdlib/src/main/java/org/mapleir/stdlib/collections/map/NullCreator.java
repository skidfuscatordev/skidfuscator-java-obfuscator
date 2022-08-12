package org.mapleir.stdlib.collections.map;

public class NullCreator<V> implements ValueCreator<V> {

	@Override
	public V create() {
		return null;
	}
}