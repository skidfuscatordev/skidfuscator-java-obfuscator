package org.mapleir.stdlib.collections.map;

public interface ValueCreator<V> extends KeyedValueCreator<Object, V> {
	V create();

	@Override
	default V create(Object o) {
		return create();
	}
}