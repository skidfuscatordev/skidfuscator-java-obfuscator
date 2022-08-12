package org.mapleir.propertyframework.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.api.event.container.PropertyAddedEvent;
import org.mapleir.propertyframework.api.event.container.PropertyRemovedEvent;
import org.mapleir.propertyframework.util.PropertyHelper;

import com.google.common.eventbus.EventBus;

public class BasicPropertyDictionary implements IPropertyDictionary {

	private final Map<String, IProperty<?>> map = new HashMap<>();
	private final EventBus bus;

	public BasicPropertyDictionary() {
		this(PropertyHelper.getFrameworkBus());
	}

	public BasicPropertyDictionary(EventBus bus) {
		this.bus = bus;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IProperty<T> find(String key) {
		if (!map.containsKey(key)) {
			return null;
		}

		IProperty<?> prop = map.get(key);
		return (IProperty<T>) prop;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IProperty<T> find(Class<T> type, String key) {
		if (!map.containsKey(key)) {
			return null;
		}

		IProperty<?> prop = map.get(key);
		if (type == null || type.isAssignableFrom(prop.getType())) {
			return (IProperty<T>) prop;
		} else {
			Class<?> rebasedType = PropertyHelper.rebasePrimitiveType(type);
			if (prop.getType().equals(rebasedType)) {
				return (IProperty<T>) prop;
			} else {
				/*
				 * New specification compliant: see IPropertyDictionary.find(Class, Key) docs
				 * throw new IllegalStateException(String.format("Cannot coerce %s to %s",
				 * prop.getType(), type));
				 */
				return null;
			}
		}
	}

	private void checkNotHeld(IProperty<?> p) {
		if(p.getDictionary() != null && p.getDictionary() != this) {
			throw new UnsupportedOperationException("Cannot link property to another dictionary");
		}
	}
	
	@Override
	public void put(String key, IProperty<?> property) {
		if (key == null || property == null) {
			throw new NullPointerException(String.format("Cannot map %s to %s", key, property));
		}

		checkNotHeld(property);
		IProperty<?> prev = map.put(key, property);
		
		if(prev != null) {
			bus.post(new PropertyRemovedEvent(prev, this, key));
		}
		
		bus.register(property);
		bus.post(new PropertyAddedEvent(property, this, key));
	}

	@Override
	public EventBus getContainerEventBus() {
		return bus;
	}

	@Override
	public Iterator<Entry<String, IProperty<?>>> iterator() {
		return map.entrySet().iterator();
	}
}