package org.mapleir.serviceframework.api;

public interface IServiceReference<T> {

	Class<T> getServiceType();
	
	IServiceRegistry getServiceRegistry();
	
	IServiceContext getContext();
}