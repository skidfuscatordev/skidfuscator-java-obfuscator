package org.mapleir.propertyframework.api.event;

import org.mapleir.propertyframework.api.IProperty;

public interface IPropertyEvent {

	IProperty<?> getProperty();
}