package org.mapleir.serviceframework.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.serviceframework.api.IServiceContext;
import org.mapleir.serviceframework.api.IServiceFactory;
import org.mapleir.serviceframework.api.IServiceQuery;
import org.mapleir.serviceframework.api.IServiceReference;
import org.mapleir.serviceframework.api.IServiceReferenceHandler;
import org.mapleir.serviceframework.api.IServiceReferenceHandlerFactory;
import org.mapleir.serviceframework.api.IServiceRegistry;
import org.mapleir.serviceframework.util.ClassServiceQuery;

// FIXME: synchronise data structures
public class ServiceRegistryImpl implements IServiceRegistry {

	private static final Logger LOGGER = Logger.getLogger(ServiceRegistryImpl.class);

	private final ContextLookupHelper map = new ContextLookupHelper();
	private final IServiceReferenceHandlerFactory serviceReferenceFactoryHandler = new ServiceReferenceHandlerFactoryImpl();

	@Override
	public <T> IServiceReference<T> getServiceReference(IServiceContext context, Class<T> clazz) {
		Collection<IServiceReference<T>> col = getServiceReferences(context, clazz, new ClassServiceQuery<>(clazz));
		if (col != null && !col.isEmpty()) {
			return col.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public <T> Collection<IServiceReference<T>> getServiceReferences(IServiceContext context, Class<T> clazz,
			IServiceQuery<T> query) {
		List<IServiceReference<T>> refs = map.get(context, clazz);
		if (refs == null || refs.isEmpty()) {
			return null;
		}

		List<IServiceReference<T>> newList = new ArrayList<>();
		for (IServiceReference<T> ref : refs) {
			if (query.accept(ref)) {
				newList.add(ref);
			}
		}

		return newList;
	}

	@Override
	public <T> void registerService(IServiceContext context, Class<T> clazz, T obj) {
		IServiceReference<T> ref = serviceReferenceFactoryHandler.createReference(this, context, clazz, obj);
		if (ref != null) {
			map.get(context, clazz).add(ref);
		} else {
			LOGGER.error(
					String.format("Couldn't create service reference for %s (%s), context=%s", obj, clazz, context));
		}
	}

	@Override
	public <T> void registerServiceFactory(IServiceContext context, Class<T> clazz, IServiceFactory<T> factory) {
		IServiceReference<T> ref = serviceReferenceFactoryHandler.createFactoryReference(this, context, clazz, factory);
		if (ref != null) {
			map.get(context, clazz).add(ref);
		} else {
			LOGGER.error(String.format("Couldn't create service factory reference for %s (%s), context=%s", factory,
					clazz, context));
		}
	}

	@Override
	public <T> T getService(IServiceReference<T> ref, IPropertyDictionary dict) {
		if (ref != null) {
			IServiceReferenceHandler handler = serviceReferenceFactoryHandler.findReferenceHandler(ref);
			return handler.loadService(ref, dict);
		} else {
			LOGGER.error("Tried to get service without reference");
		}

		return null;
	}

	@Override
	public void ungetService(IServiceReference<?> ref) {
		if (ref != null) {
			IServiceReferenceHandler handler = serviceReferenceFactoryHandler.findReferenceHandler(ref);
			handler.unloadService(ref);
		} else {
			LOGGER.error("Tried to unget service without reference");
		}
	}

	private static class ContextLookupHelper {
		private final Map<IServiceContext, ServiceMap> contextMaps = new HashMap<>();

		public <T> List<IServiceReference<T>> get(IServiceContext context, Class<T> key) {
			if (context == null)
				throw new NullPointerException("Need context for lookup");
			if (key == null)
				throw new NullPointerException("No key for service maps");

			ServiceMap map = contextMaps.get(context);
			if (map == null) {
				map = new ServiceMap();
				contextMaps.put(context, map);
			}
			return map.get(key);
		}

		private static class ServiceMap {
			private final Map<Class<?>, List<IServiceReference<?>>> registeredServiceReferences = new HashMap<>();

			@SuppressWarnings({ "rawtypes", "unchecked" })
			public <T> List<IServiceReference<T>> get(Class<T> key) {
				if (key == null)
					throw new NullPointerException("No key for service list");

				List<IServiceReference<T>> list = (List) registeredServiceReferences.get(key);
				if (list == null) {
					list = new ArrayList<>();
					registeredServiceReferences.put(key, (List) list);
				}
				return list;
			}
		}
	}
}