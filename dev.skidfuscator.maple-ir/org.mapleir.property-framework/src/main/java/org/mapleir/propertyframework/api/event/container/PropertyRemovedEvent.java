package org.mapleir.propertyframework.api.event.container;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;

public class PropertyRemovedEvent extends AbstractPropertyContainerEvent {

	public PropertyRemovedEvent(IProperty<?> prop, IPropertyDictionary dictionary, String key) {
		super(prop, dictionary, key);
	}
}