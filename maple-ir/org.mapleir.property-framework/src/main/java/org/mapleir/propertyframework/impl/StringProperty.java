package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;

public class StringProperty extends DefaultValueProperty<String> {

	public StringProperty(String key) {
		super(key, String.class, "");
	}
	
	public StringProperty(String key, String dflt) {
		super(key, String.class, dflt);
	}

	public StringProperty(String key, String dflt, String val) {
		super(key, String.class, dflt);
		setValue(val);
	}
	
	@Override
	public StringProperty clone(IPropertyDictionary newDict) {
		return new StringProperty(getKey(), getDefault(), getValue());
	}
}