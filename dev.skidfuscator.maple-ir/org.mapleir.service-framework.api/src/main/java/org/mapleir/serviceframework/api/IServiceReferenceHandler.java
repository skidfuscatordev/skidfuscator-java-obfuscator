package org.mapleir.serviceframework.api;

import org.mapleir.propertyframework.api.IPropertyDictionary;

public interface IServiceReferenceHandler {

	<T> T loadService(IServiceReference<T> ref, IPropertyDictionary dict);

	void unloadService(IServiceReference<?> ref);
}