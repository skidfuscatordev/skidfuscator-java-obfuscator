package org.mapleir.serviceframework.api;

import java.util.Collection;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.util.PropertyHelper;

public interface IServiceRegistry {

	<T> IServiceReference<T> getServiceReference(IServiceContext cxt, Class<T> clazz);
	
	<T> Collection<IServiceReference<T>> getServiceReferences(IServiceContext cxt, Class<T> clazz, IServiceQuery<T> query);
	
	<T> void registerService(IServiceContext cxt, Class<T> clazz, T obj);
	
	<T> void registerServiceFactory(IServiceContext cxt, Class<T> clazz, IServiceFactory<T> factory);

	default <T> T getService(IServiceReference<T> ref) {
		return getService(ref, PropertyHelper.getImmutableDictionary());
	}
	
	<T> T getService(IServiceReference<T> ref, IPropertyDictionary dict);
	
	void ungetService(IServiceReference<?> ref);
}