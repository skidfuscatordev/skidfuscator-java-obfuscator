package org.mapleir.dot4j.attr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MapAttrs implements Attrs, Iterable<Entry<String, Object>> {

	private final Map<String, Object> attributes;
	
	public MapAttrs() {
		attributes = new HashMap<>();
	}
	
	public MapAttrs put(String key, Object value) {
		if(value != null) {
			attributes.put(key, value);
		} else {
			attributes.remove(value);
		}
		return this;
	}
	
	public MapAttrs putAll(MapAttrs mapAttrs) {
		attributes.putAll(mapAttrs.attributes);
		return this;
	}
	
	@Override
	public boolean isEmpty() {
		return attributes.isEmpty();
	}
	
	@Override
	public Object get(String key) {
		return attributes.get(key);
	}
	
	@Override
	public Attrs applyTo(MapAttrs mapAttrs) {
		mapAttrs.attributes.putAll(attributes);
		return mapAttrs;
	}
	
	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return attributes.entrySet().iterator();
	}

	@Override
	public int hashCode() {
		return attributes.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapAttrs other = (MapAttrs) obj;
		return attributes.equals(other.attributes);
	}
	
	@Override
	public String toString() {
		return attributes.toString();
	}
}
