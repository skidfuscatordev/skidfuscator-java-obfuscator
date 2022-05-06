package org.mapleir.serviceframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.serviceframework.api.IServiceContext;
import org.mapleir.serviceframework.api.IServiceFactory;
import org.mapleir.serviceframework.api.IServiceRegistry;

public class InternalFactoryServiceReferenceImpl<T> extends AbstractInternalServiceReference<T> {

	private final IServiceFactory<T> factory;

	public InternalFactoryServiceReferenceImpl(IServiceRegistry serviceRegistry, IServiceContext serviceContext,
			Class<T> serviceType, IServiceFactory<T> factory) {
		super(serviceRegistry, serviceContext, serviceType);
		this.factory = factory;
	}

	@Override
	public T get(IPropertyDictionary dict) {
		return factory.create(dict);
	}
}