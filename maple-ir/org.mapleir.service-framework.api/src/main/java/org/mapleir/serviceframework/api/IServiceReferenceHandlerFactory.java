package org.mapleir.serviceframework.api;

public interface IServiceReferenceHandlerFactory {

	<T> IServiceReference<T> createReference(IServiceRegistry serviceRegistry, IServiceContext serviceContext,
			Class<T> serviceType, T obj);
	
	<T> IServiceReference<T> createFactoryReference(IServiceRegistry serviceRegistry, IServiceContext serviceContext,
			Class<T> serviceType, IServiceFactory<T> factory);
	
	IServiceReferenceHandler findReferenceHandler(IServiceReference<?> ref);
}