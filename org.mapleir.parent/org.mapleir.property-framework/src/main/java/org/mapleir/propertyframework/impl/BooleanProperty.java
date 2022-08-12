package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;

public class BooleanProperty extends DefaultValueProperty<Boolean> {

	public BooleanProperty(String key) {
		this(key, false);
	}
	
	public BooleanProperty(String key, boolean dflt) {
		super(key, Boolean.class, dflt);
	}
	
	@Override
	public BooleanProperty clone(IPropertyDictionary newDict) {
		BooleanProperty p = new BooleanProperty(getKey(), getDefault());
		p.setValue(getValue());
		return p;
	}
}