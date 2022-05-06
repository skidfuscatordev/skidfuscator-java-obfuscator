package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;

public class DefaultValueProperty<T> extends AbstractProperty<T> {

	private final T dflt;
	
	public DefaultValueProperty(String key, Class<T> type, T dflt) {
		super(key, type);
		this.dflt = dflt;
	}

	@Override
	public T getDefault() {
		return dflt;
	}

	@Override
	public DefaultValueProperty<T> clone(IPropertyDictionary newDict) {
		DefaultValueProperty<T> p = new DefaultValueProperty<T>(getKey(), getType(), dflt);
		p.setValue(getValue());
		return p;
	}
}