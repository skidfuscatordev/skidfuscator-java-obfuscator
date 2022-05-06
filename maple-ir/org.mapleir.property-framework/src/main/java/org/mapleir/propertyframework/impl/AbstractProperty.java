package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.api.event.container.PropertyAddedEvent;
import org.mapleir.propertyframework.api.event.container.PropertyRemovedEvent;
import org.mapleir.propertyframework.api.event.update.PropertyValueChangedEvent;

import com.google.common.eventbus.Subscribe;

public abstract class AbstractProperty<T> implements IProperty<T> {

	private final String key;
	private final Class<T> type;
	
	private IPropertyDictionary container;
	private T value;
	
	public AbstractProperty(String key, Class<T> type) {
		this.key = key;
		this.type = type;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public IPropertyDictionary getDictionary() {
		return container;
	}

	@Override
	public void tryLinkDictionary(IPropertyDictionary dict) {
		if(this.container == null) {
			this.container = dict;
			
			if(dict.find(getType(), getKey()) == null) {
				/* actually use this one */
				dict.put(this);
			} else {
				/* just set it up for listening */
				dict.getContainerEventBus().register(this);
			}
		}
	}
	
	@Override
	public T getValue() {
		IProperty<T> del = getDelegate();
		if(del != null) {
			return del.getValue();
		} else if(value == null) {
			value = getDefault();
			
			if(container != null) {
				container.getContainerEventBus().post(new PropertyValueChangedEvent(this, null, value));
			}
		}
		return value;
	}

	@Override
	public void setValue(T t) {
		IProperty<T> del = getDelegate();
		if(del != null) {
			del.setValue(t);
		} else {
			T old = value;
			value = t;
			
			if(container != null) {
				container.getContainerEventBus().post(new PropertyValueChangedEvent(this, old, value));
			}
		}
	}
	
	protected IProperty<T> getDelegate() {
		if(container == null) {
			return null;
		}
		
		IProperty<T> prop = container.find(type, key);
		if(prop == null || prop == this) {
			return null;
		} else {
			return prop;
		}
	}
	
	@Subscribe
	public void onPropertyAddedEvent(PropertyAddedEvent e) {
		if(!getKey().equals(e.getKey())) {
			return;
		}
		
		if(container != null) {
			// TODO: log
			// throw new UnsupportedOperationException("Tried to add container-held property to another container");
		} else {
			container = e.getDictionary();
		}
	}
	
	@Subscribe
	public void onPropertyRemovedEvent(PropertyRemovedEvent e) {
		if(!getKey().equals(e.getKey())) {
			return;
		}
		
		if(container != null) {
			container.getContainerEventBus().unregister(this);
			container = null;
		} else {
			// TODO: log
			// throw new UnsupportedOperationException("Tried to remove containerless property from container");
		}
	}
	
	public abstract T getDefault();
}