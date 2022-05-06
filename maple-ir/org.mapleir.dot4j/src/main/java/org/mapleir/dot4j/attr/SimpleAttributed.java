package org.mapleir.dot4j.attr;

import java.util.Iterator;
import java.util.Map.Entry;

public class SimpleAttributed<E> implements Attributed<E> {

	private final E delegate;
	private final MapAttrs attrs;
	
	public SimpleAttributed(E delegate) {
		this.delegate = delegate;
		attrs = new MapAttrs();
	}
	
	public SimpleAttributed(E delegate, Attrs attrs) {
		this.delegate = delegate;
		this.attrs = new MapAttrs();
		if(attrs != null) {
			attrs.applyTo(this.attrs);
		}
	}
	
	@Override
	public Attrs applyTo(MapAttrs mapAttrs) {
		return mapAttrs.putAll(attrs);
	}

	@Override
	public E with(Attrs attrs) {
		attrs.applyTo(this.attrs);
		return delegate;
	}

	@Override
	public Object get(String key) {
		return attrs.get(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleAttributed<?> other = (SimpleAttributed<?>) obj;
		if (attrs == null) {
			if (other.attrs != null)
				return false;
		} else if (!attrs.equals(other.attrs))
			return false;
		if (delegate == null) {
			if (other.delegate != null)
				return false;
		} else if (!delegate.equals(other.delegate))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return attrs.hashCode();
	}
	
	@Override
	public String toString() {
		return attrs.toString();
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return attrs.iterator();
	}
}
