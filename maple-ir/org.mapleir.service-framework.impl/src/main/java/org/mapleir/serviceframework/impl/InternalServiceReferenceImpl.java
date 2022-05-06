package org.mapleir.serviceframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.serviceframework.api.IServiceContext;
import org.mapleir.serviceframework.api.IServiceRegistry;

public class InternalServiceReferenceImpl<T> extends AbstractInternalServiceReference<T> {

	private final T obj;

	public InternalServiceReferenceImpl(IServiceRegistry serviceRegistry, IServiceContext serviceContext,
			Class<T> serviceType, T obj) {
		super(serviceRegistry, serviceContext, serviceType);
		this.obj = obj;
	}

	@Override
	public T get(IPropertyDictionary dict) {
		return obj;
	}
}