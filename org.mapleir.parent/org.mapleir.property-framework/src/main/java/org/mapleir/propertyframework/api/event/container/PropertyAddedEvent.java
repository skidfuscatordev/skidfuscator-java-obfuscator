package org.mapleir.propertyframework.api.event.container;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;

public class PropertyAddedEvent extends AbstractPropertyContainerEvent {

	public PropertyAddedEvent(IProperty<?> prop, IPropertyDictionary dictionary, String key) {
		super(prop, dictionary, key);
	}
}