package org.topdank.eventbus;



public class ObjectEvent<T> implements Event {

	private final T t;

	public ObjectEvent(T t) {
		this.t = t;
	}

	public T get() {
		return t;
	}
}