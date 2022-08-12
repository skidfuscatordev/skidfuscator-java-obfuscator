package org.topdank.eventbus.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.topdank.eventbus.BusRegistry;
import org.topdank.eventbus.Event;
import org.topdank.eventbus.EventBus;
import org.topdank.eventbus.EventPriority;
import org.topdank.eventbus.EventTarget;
import org.topdank.eventbus.util.ReflectionHelper;

public class BasicEventBus implements EventBus {

	private final Set<CallbackData> registered;

	public BasicEventBus() {
		registered = new HashSet<CallbackData>();
	}

	@Override
	public void register(Object src) {
		if (src == null)
			return;

		if (src instanceof Class) {
			registerStatic((Class<?>) src);
		} else {
			registerObject(src);
		}

		// System.out.println(registered);
	}

	private void registerObject(Object obj) {
		Class<?> c = obj.getClass();
		List<Method> methods = ReflectionHelper.deepCollateMethods(c);
		for (Method m : methods) {
			if (!Modifier.isStatic(m.getModifiers())) {
				EventTarget manifest = m.getAnnotation(EventTarget.class);
				if (manifest != null) {
					Class<? extends Event> eventKlass = valid(m);
					if (eventKlass != null) {
						VirtualCallbackData data = new VirtualCallbackData(m, eventKlass, obj);
						registered.add(data);
					}
				}
			}
		}
	}

	private void registerStatic(Class<?> c) {
		Collection<Class<?>> classes = ReflectionHelper.getSuperClasses(c);
		List<Method> methods = ReflectionHelper.deepCollateMethods(classes, c);
		for (Method m : methods) {
			if (Modifier.isStatic(m.getModifiers())) {
				EventTarget manifest = m.getAnnotation(EventTarget.class);
				if (manifest != null) {
					Class<? extends Event> eventKlass = valid(m);
					if (eventKlass != null) {
						StaticCallbackData data = new StaticCallbackData(m, eventKlass);
						registered.add(data);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Event> valid(Method m) {
		Parameter[] params = m.getParameters();
		if (params.length != 1)
			return null;
		Class<?> klass = params[0].getType();
		if (Event.class.isAssignableFrom(klass))
			return (Class<? extends Event>) klass;
		return null;
	}

	@Override
	public void register(Object src, @SuppressWarnings("unchecked") Class<? extends Event>... eventClass) {
		if (src == null)
			return;

		if (src instanceof Class) {
			registerStatic((Class<?>) src);
		} else {
			registerObject(src);
		}
	}

	@Override
	public void unregister(Object src) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unregister(Object src, @SuppressWarnings("unchecked") Class<? extends Event>... eventClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dispatch(Event... events) {
		for (Event e : events) {
			Class<?> eventKlass = e.getClass();
			for (CallbackData d : registered) {
				if (d.same(eventKlass)) {
					try {
						d.invoke(e);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}

	static abstract class CallbackData {
		final Method method;
		final Class<? extends Event> eventKlass;

		public CallbackData(Method method, Class<? extends Event> eventKlass) {
			this.method = method;
			method.setAccessible(true);
			this.eventKlass = eventKlass;
		}

		public boolean same(Object o) {
			if (o instanceof Class)
				return eventKlass.isAssignableFrom((Class<?>) o);
			return false;
		}

		public abstract void invoke(Event e) throws Exception;
	}

	static class VirtualCallbackData extends CallbackData {
		private final Object obj;

		public VirtualCallbackData(Method method, Class<? extends Event> eventKlass, Object obj) {
			super(method, eventKlass);
			this.obj = obj;
		}

		@Override
		public boolean same(Object o) {
			if (o instanceof Class)
				return eventKlass.isAssignableFrom((Class<?>) o);
			return obj.equals(o);
		}

		@Override
		public void invoke(Event e) throws Exception {
			method.invoke(obj, e);
		}

		@Override
		public String toString() {
			String name = obj == null ? "null" : obj.getClass().getCanonicalName();
			String mName = method.getName();
			String eName = eventKlass.getSimpleName();
			return "VirtualCallbackData [obj=" + name + ", method=" + mName + ", eventKlass=" + eName + "]";
		}
	}

	static class StaticCallbackData extends CallbackData {

		public StaticCallbackData(Method method, Class<? extends Event> klass) {
			super(method, klass);
		}

		@Override
		public void invoke(Event e) throws Exception {
			method.invoke(null, e);
		}
	}

	static class DefaultSetMap<T> extends HashMap<T, Set<CallbackData>> {
		private static final long serialVersionUID = 1797882769602866826L;

		@Override
		public Set<CallbackData> get(Object o) {
			Set<CallbackData> d = super.get(o);
			if (d == null)
				d = new HashSet<CallbackData>();
			return d;
		}
	}

	@EventTarget(priority = EventPriority.HIGHEST)
	public void test(Event e) {
		System.out.println("e: " + e);
	}

	public static void main(String[] args) {
		EventBus bus = new BasicEventBus();
		bus.register(bus);
		Event e = new Event() {
		};
		BusRegistry.getInstance().getGlobalBus().dispatch(e);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getCanonicalName()).append("\n");
		Iterator<CallbackData> it = registered.iterator();
		while (it.hasNext()) {
			CallbackData d = it.next();
			sb.append("\t").append(d);
			if (it.hasNext())
				sb.append("\n");
		}
		return sb.toString();
	}
}