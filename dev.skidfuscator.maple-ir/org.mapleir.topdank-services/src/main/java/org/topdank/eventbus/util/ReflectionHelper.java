package org.topdank.eventbus.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ReflectionHelper {

	public static final Filter<Method> ACCEPT_ALL = new Filter<Method>() {
		@Override
		public boolean accept(Method m) {
			return true;
		}
	};

	private static final Filter<Class<?>> JAVA_CLASS_FILTER = new Filter<Class<?>>() {
		@Override
		public boolean accept(Class<?> c) {
			if (c == null)
				return false;
			String name = c.getCanonicalName();
			return name.startsWith("java") || name.startsWith("com.sun");
		}
	};

	public static final List<Class<?>> getSuperClasses(Class<?> c) {
		return getSuperClasses(c, JAVA_CLASS_FILTER);
	}

	public static final List<Class<?>> getSuperClasses(Class<?> c, Filter<Class<?>> filter) {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add(c);
		while (c != null) {
			c = c.getSuperclass();
			if ((c == null) || filter.accept(c))
				break;
			classes.add(c);
		}
		return classes;
	}

	public static final List<Method> collateMethods(Class<?> c, Filter<Method> filter) {
		List<Method> methods = new ArrayList<Method>();
		Method[] ms = c.getDeclaredMethods();
		for (Method m : ms)
			methods.add(m);
		return methods;
	}

	public static final List<Method> collateMethods(Class<?> c) {
		return collateMethods(c, ACCEPT_ALL);
	}

	public static final List<Method> deepCollateMethods(Collection<Class<?>> classes, Class<?> c) {
		List<Method> methods = new ArrayList<Method>();
		for (Class<?> c1 : classes) {
			methods.addAll(collateMethods(c1));
		}
		return methods;
	}

	public static final List<Method> deepCollateMethods(Class<?> c) {
		return deepCollateMethods(getSuperClasses(c), c);
	}

	public static abstract interface Filter<T> {
		public abstract boolean accept(T t);
	}
}