package org.mapleir.serviceframework.util;

import org.mapleir.serviceframework.api.IServiceQuery;
import org.mapleir.serviceframework.api.IServiceReference;

public class ClassServiceQuery<T> implements IServiceQuery<T> {

	private final Class<T> clazz;
	
	public ClassServiceQuery(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public boolean accept(IServiceReference<T> ref) {
		return clazz.equals(ref.getServiceType());
	}
}