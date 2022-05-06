package org.topdank.eventbus;

import java.lang.reflect.Method;

public final class EventMethodData {

	public EventPriority priority;
	public Object src;
	public Method method;

	public EventMethodData(EventPriority priority, Object src, Method method) {
		super();
		this.priority = priority;
		this.src = src;
		this.method = method;
		if (!method.isAccessible())
			method.setAccessible(true);
	}
}