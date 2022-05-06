package org.mapleir.serviceframework.api;

import org.mapleir.propertyframework.api.IPropertyDictionary;

public interface IServiceFactory<T> {

	T create(IPropertyDictionary dict);
}