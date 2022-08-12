package org.mapleir.serviceframework.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.serviceframework.api.IServiceContext;
import org.mapleir.serviceframework.api.IServiceRegistry;

public abstract class AbstractInternalServiceReference<T> implements IInternalServiceReference<T> {

	private final AtomicInteger monitors;
	private final Class<T> serviceType;
	private final IServiceRegistry serviceRegistry;
	private final IServiceContext serviceContext;

	public AbstractInternalServiceReference(IServiceRegistry serviceRegistry, IServiceContext serviceContext,
			Class<T> serviceType) {
		this.monitors = new AtomicInteger();
		this.serviceRegistry = serviceRegistry;
		this.serviceContext = serviceContext;
		this.serviceType = serviceType;
	}

	@Override
	public abstract T get(IPropertyDictionary dict);

	@Override
	public IServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public IServiceContext getContext() {
		return serviceContext;
	}

	@Override
	public Class<T> getServiceType() {
		return serviceType;
	}

	@Override
	public void lock() {
		monitors.incrementAndGet();
	}

	@Override
	public void unlock() {
		monitors.decrementAndGet();
	}

	@Override
	public boolean locked() {
		return monitors.get() > 0;
	}
}