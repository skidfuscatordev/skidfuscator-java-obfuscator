package org.mapleir.dot4j.attr;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;

public class Attr<T> implements Attrs {
	
	protected final String key;
	protected final T value;
	
	public Attr(String key, T value) {
		this.key = key;
		this.value = value;
	}
	
	public <E> E key(String key) {
		return newInstance(key, value);
	}
	
	public <E> E value(T value) {
		return newInstance(key, value);
	}

    @SuppressWarnings("unchecked")
	private <E> E newInstance(String key, T value) {
        try {
            final ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
            final Class<?> type = (Class<?>) superclass.getActualTypeArguments()[0];
            @SuppressWarnings("rawtypes")
			final Constructor<? extends Attr> cons = getClass().getDeclaredConstructor(String.class, type);
            cons.setAccessible(true);
            return (E) cons.newInstance(key, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

	@Override
	public Attrs applyTo(MapAttrs mapAttrs) {
		return mapAttrs.put(key, value);
	}
}
