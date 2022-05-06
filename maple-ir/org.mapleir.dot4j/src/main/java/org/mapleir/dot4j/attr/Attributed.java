package org.mapleir.dot4j.attr;

import java.util.Map.Entry;

public interface Attributed<T> extends Attrs, Iterable<Entry<String, Object>> {

	default T with(String name, Object value) {
		return with(Attrs.attr(name, value));
	}
	
	default T with(Attrs... attrs) {
		return with(Attrs.attrs(attrs));
	}
	
	T with(Attrs attrs);
}
