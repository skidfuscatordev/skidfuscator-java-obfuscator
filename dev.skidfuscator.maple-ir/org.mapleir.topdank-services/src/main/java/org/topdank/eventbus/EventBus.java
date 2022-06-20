package org.topdank.eventbus;

public abstract interface EventBus {

	public abstract void register(Object src);

	public abstract void register(Object src, @SuppressWarnings("unchecked") Class<? extends Event>... eventClass);

	public abstract void unregister(Object src);

	public abstract void unregister(Object src, @SuppressWarnings("unchecked") Class<? extends Event>... eventClass);

	public abstract void dispatch(Event... events);
}
