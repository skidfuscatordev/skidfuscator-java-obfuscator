package org.mapleir.stdlib.collections.map;

import java.util.LinkedHashMap;

/**
 * @author Bibl (don't ban me pls)
 */
public class NullPermeableLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = 1L;

	private final ValueCreator<V> creator;

	public NullPermeableLinkedHashMap(ValueCreator<V> creator) {
		this.creator = creator;
	}

	public NullPermeableLinkedHashMap() {
		this(new NullCreator<>());
	}

	public V getNotNull(K k) {
		return computeIfAbsent(k, creator::create);
	}
}
