package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IProperty;

public class BasicSynchronisedPropertyDictionary extends BasicPropertyDictionary {

	private final Object mapLock = new Object();

	@Override
	public <T> IProperty<T> find(String key) {
		synchronized (mapLock) {
			return super.find(key);
		}
	}

	@Override
	public <T> IProperty<T> find(Class<T> type, String key) {
		synchronized (mapLock) {
			return super.find(type, key);
		}
	}

	@Override
	public void put(String key, IProperty<?> property) {
		synchronized (mapLock) {
			super.put(key, property);
		}
	}
}