package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;

public class NumberProperty extends DefaultValueProperty<Number> {

	public NumberProperty(String key) {
		super(key, Number.class, 0);
	}

	public NumberProperty(String key, Number dflt) {
		super(key, Number.class, dflt);
	}

	public float getFloat() {
		Number n = getValue();
		if (n != null) {
			return n.floatValue();
		} else {
			return 0;
		}
	}

	public double getDouble() {
		Number n = getValue();
		if (n != null) {
			return n.doubleValue();
		} else {
			return 0;
		}
	}

	public int getInt() {
		Number n = getValue();
		if (n != null) {
			return n.intValue();
		} else {
			return 0;
		}
	}

	public byte getByte() {
		Number n = getValue();
		if (n != null) {
			return n.byteValue();
		} else {
			return 0;
		}
	}

	public short getShort() {
		Number n = getValue();
		if (n != null) {
			return n.shortValue();
		} else {
			return 0;
		}
	}

	public long getLong() {
		Number n = getValue();
		if (n != null) {
			return n.longValue();
		} else {
			return 0;
		}
	}
	
	@Override
	public NumberProperty clone(IPropertyDictionary newDict) {
		NumberProperty n = new NumberProperty(getKey(), getDefault());
		n.setValue(getValue());
		return n;
	}
}