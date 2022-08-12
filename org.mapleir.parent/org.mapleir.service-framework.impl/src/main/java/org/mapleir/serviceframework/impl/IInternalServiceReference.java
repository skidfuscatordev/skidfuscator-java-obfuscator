package org.mapleir.serviceframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.serviceframework.api.IServiceReference;

public interface IInternalServiceReference<T> extends IServiceReference<T> {

	T get(IPropertyDictionary dict);
	
	void lock();
	
	void unlock();
	
	boolean locked();
}