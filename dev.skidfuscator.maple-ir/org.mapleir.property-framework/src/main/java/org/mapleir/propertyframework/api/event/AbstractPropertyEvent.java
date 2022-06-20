package org.mapleir.propertyframework.api.event;

import org.mapleir.propertyframework.api.IProperty;

public abstract class AbstractPropertyEvent implements IPropertyEvent {

	private final IProperty<?> prop;

	public AbstractPropertyEvent(IProperty<?> prop) {
		this.prop = prop;
	}

	@Override
	public IProperty<?> getProperty() {
		return prop;
	}
}