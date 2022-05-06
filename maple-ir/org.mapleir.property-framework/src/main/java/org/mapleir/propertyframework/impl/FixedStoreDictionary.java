package org.mapleir.propertyframework.impl;

import java.util.Map.Entry;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;

/**
 * A dictionary implementation that does not allow new keys to be added. This is
 * different from the pure immutable variant in PropertyHelper as the map may
 * contain values (which are added during initialisation). The values of these
 * properties may also change.
 * 
 * @author Bibl
 */
public class FixedStoreDictionary extends BasicPropertyDictionary {

	public FixedStoreDictionary(IPropertyDictionary dict) {
		for(Entry<String, IProperty<?>> e : dict) {
			super.put(e.getKey(), e.getValue().clone(this));
		}
	}
	
	@Override
	public void put(String key, IProperty<?> property) {
		throw new UnsupportedOperationException("Cannot put; it's a fixedstore");
	}
}