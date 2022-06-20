package org.mapleir.serviceframework.impl;

import java.util.HashMap;
import java.util.Map;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.serviceframework.api.IServiceContext;
import org.mapleir.serviceframework.api.IServiceFactory;
import org.mapleir.serviceframework.api.IServiceReference;
import org.mapleir.serviceframework.api.IServiceReferenceHandler;
import org.mapleir.serviceframework.api.IServiceReferenceHandlerFactory;
import org.mapleir.serviceframework.api.IServiceRegistry;

public class ServiceReferenceHandlerFactoryImpl implements IServiceReferenceHandlerFactory {

	@SuppressWarnings("rawtypes")
	private final Map<Class<? extends IServiceReference>, IServiceReferenceHandler> handlers = new HashMap<>();

	public ServiceReferenceHandlerFactoryImpl() {
		IServiceReferenceHandler h = new DefaultAbstractInternalServiceReferenceServiceReferenceHandler();
		handlers.put(InternalServiceReferenceImpl.class, h);
		handlers.put(InternalFactoryServiceReferenceImpl.class, h);
	}

	// TODO: more handler map methods
	public void addHandler(Class<? extends IServiceReference<?>> clazz, IServiceReferenceHandler handler) {
		if (clazz == null)
			throw new NullPointerException(String.format("No class key for handler: %s", handler));
		if (handler == null)
			throw new NullPointerException(String.format("No associated handler for class key: %s", clazz));
		if (handlers.containsKey(clazz)) {
			throw new IllegalStateException(String.format("Cannot reregister handler for %s old=%s, new=%s", clazz,
					handlers.get(clazz), handler));
		} else {
			handlers.put(clazz, handler);
		}
	}

	@Override
	public <T> IServiceReference<T> createReference(IServiceRegistry serviceRegistry, IServiceContext serviceContext,
			Class<T> serviceType, T obj) {
		if (serviceRegistry == null || serviceContext == null || serviceType == null || obj == null)
			throw new NullPointerException();

		return new InternalServiceReferenceImpl<>(serviceRegistry, serviceContext, serviceType, obj);
	}

	@Override
	public <T> IServiceReference<T> createFactoryReference(IServiceRegistry serviceRegistry,
			IServiceContext serviceContext, Class<T> serviceType, IServiceFactory<T> factory) {
		if (serviceRegistry == null || serviceContext == null || serviceType == null || factory == null)
			throw new NullPointerException();

		return new InternalFactoryServiceReferenceImpl<>(serviceRegistry, serviceContext, serviceType, factory);
	}

	@Override
	public IServiceReferenceHandler findReferenceHandler(IServiceReference<?> ref) {
		if (ref == null)
			throw new NullPointerException();

		@SuppressWarnings("rawtypes")
		Class<? extends IServiceReference> refClazz = ref.getClass();

		if (handlers.containsKey(refClazz)) {
			return handlers.get(refClazz);
		} else {
			throw new UnsupportedOperationException(
					String.format("No service handler for service reference type: %s", refClazz));
		}
	}

	private static class DefaultAbstractInternalServiceReferenceServiceReferenceHandler
			implements IServiceReferenceHandler {

		@Override
		public <T> T loadService(IServiceReference<T> _ref, IPropertyDictionary dict) {
			AbstractInternalServiceReference<T> ref = (AbstractInternalServiceReference<T>) _ref;
			T o = ref.get(dict);
			ref.lock();

			return o;
		}

		@Override
		public void unloadService(IServiceReference<?> _ref) {
			((AbstractInternalServiceReference<?>) _ref).unlock();
		}
	}
}