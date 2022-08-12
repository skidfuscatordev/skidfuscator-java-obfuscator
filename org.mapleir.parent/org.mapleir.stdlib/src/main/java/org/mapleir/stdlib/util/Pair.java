package org.mapleir.stdlib.util;

// todo: tuple class

import java.util.Objects;

/**
 * Represents a pair of two objects. Borrowed generously from javafx
 * @param <K> the key (first object)
 * @param <V> the value (second object)
 */
public class Pair<K, V> {
	private K key;
	private V value;

	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "(" + key + ", " + value + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Pair<?, ?> pair = (Pair<?, ?>) o;
		return Objects.equals(key, pair.key) &&
				Objects.equals(value, pair.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}
}
