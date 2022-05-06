package org.topdank.eventbus.impl;
//package org.zbot.topdank.eventbus.impl;
//
//import static org.zbot.topdank.eventbus.util.ReflectionHelper.deepCollateMethods;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//import org.zbot.topdank.eventbus.Event;
//import org.zbot.topdank.eventbus.EventBus;
//import org.zbot.topdank.eventbus.EventMethodData;
//import org.zbot.topdank.eventbus.EventPriority;
//import org.zbot.topdank.eventbus.EventStoppable;
//import org.zbot.topdank.eventbus.EventTarget;
//
///**
// * @author Bibl (don't ban me pls) <br>
// * @created 29 Mar 2015 at 22:46:11 <br>
// */
//public class DefaultEventBus implements EventBus {
//
//	protected HashMap<Class<? extends Event>, List<EventMethodData>> registeredListeners;
//
//	public DefaultEventBus() {
//		registeredListeners = new HashMap<Class<? extends Event>, List<EventMethodData>>();
//	}
//
//	/**
//	 * Registers all the methods marked with the {@link EventTarget} annotation as listeners.
//	 *
//	 * @param src
//	 *            Source object
//	 */
//	@Override
//	public void register(Object src) {
//		if (src == null)
//			return;
//		List<Method> methods = deepCollateMethods(src.getClass());
//		if (methods.size() == 0)
//			return;
//
//		System.out.println("Registered: " + src.getClass().getCanonicalName());
//
//		List<Class<? extends Event>> addedEvents = new ArrayList<Class<? extends Event>>(methods.size());
//		for (Method method : methods) {
//			if (!isValid(method))
//				continue;
//			@SuppressWarnings("unchecked")
//			Class<? extends Event> eventClass = (Class<? extends Event>) method.getParameterTypes()[0];
//			EventMethodData data = new EventMethodData(method.getAnnotation(EventTarget.class).priority(), src, method);
//			putMap(eventClass, data);
//			if (!addedEvents.contains(addedEvents))
//				addedEvents.add(eventClass);
//		}
//
//		for (Class<? extends Event> c : addedEvents)
//			prioritise(c);
//
//	}
//
//	/**
//	 * Registers all the methods marked with the {@link EventTarget} annotation that uses the appropriate event type.
//	 *
//	 * @param src
//	 *            Source object.
//	 * @param eventClass
//	 *            Appropriate event type.
//	 */
//	@Override
//	public void register(Object src, Class<? extends Event> eventClass) {
//		if (src == null)
//			return;
//
//		System.out.println("Registered: " + src.getClass().getCanonicalName());
//
//		for (Method method : deepCollateMethods(src.getClass())) {
//			if (!isValid(method))
//				continue;
//			if (!method.getParameterTypes()[0].equals(eventClass))
//				continue;
//			EventMethodData data = new EventMethodData(method.getAnnotation(EventTarget.class).priority(), src, method);
//			putMap(eventClass, data);
//		}
//		prioritise(eventClass);
//	}
//
//	/**
//	 * Registers all the methods marked with the {@link EventTarget} annotation if the event type class is included in the eventClasses.
//	 *
//	 * @param src
//	 *            Source object.
//	 * @param eventClasses
//	 *            Appropriate event types.
//	 */
//	@SuppressWarnings("unchecked")
//	@Override
//	public void register(Object src, Class<? extends Event>[] eventClasses) {
//		if (src == null)
//			return;
//		List<Method> methods = deepCollateMethods(src.getClass());
//		if (methods.size() == 0)
//			return;
//
//		System.out.println("Registered: " + src.getClass().getCanonicalName());
//
//		List<Class<? extends Event>> addedEvents = new ArrayList<Class<? extends Event>>(methods.size());
//		for (Method method : methods) {
//			if (!isValid(method))
//				continue;
//			Class<?> paramClass = method.getParameterTypes()[0];
//			boolean found = false;
//			for (Class<? extends Event> eventClass : eventClasses) {
//				if (paramClass.equals(eventClass)) {
//					found = true;
//					break;
//				}
//			}
//			if (!found)
//				continue;
//
//			EventMethodData data = new EventMethodData(method.getAnnotation(EventTarget.class).priority(), src, method);
//			Class<? extends Event> castedClass = (Class<? extends Event>) paramClass;
//			putMap(castedClass, data);
//			addedEvents.add(castedClass);
//		}
//
//		for (Class<? extends Event> c : addedEvents)
//			prioritise(c);
//	}
//
//	/**
//	 * Unregisters all of the methods that have been registered as listeners. <br>
//	 * <b>NOTE: it is faster to use the {@link #unregister(Object, Class)} method to remove specific listener types.
//	 *
//	 * @param src
//	 *            Source object.</b>
//	 */
//	@Override
//	public void unregister(Object src) {
//		if (src == null)
//			return;
//
//		System.out.println("Unregistered: " + src.getClass().getCanonicalName());
//
//		for (Class<? extends Event> eventClass : registeredListeners.keySet()) {
//			List<EventMethodData> dataList = registeredListeners.get(eventClass);
//			if (dataList == null)
//				continue;
//			ListIterator<EventMethodData> dataIt = dataList.listIterator();
//			while (dataIt.hasNext()) {
//				EventMethodData data = dataIt.next();
//				if (data.src.equals(src))
//					dataList.remove(data);
//			}
//		}
//	}
//
//	/**
//	 * Unregisters the methods that have been registered as listeners of the appropriate event type.
//	 *
//	 * @param src
//	 *            Source object
//	 * @param eventClass
//	 *            Appropriate event type.
//	 */
//	@Override
//	public void unregister(Object src, Class<? extends Event> eventClass) {
//		if (src == null)
//			return;
//		List<EventMethodData> dataList = registeredListeners.get(eventClass);
//		if (dataList == null)
//			return;
//
//		System.out.println("Unregistered: " + src.getClass().getCanonicalName());
//
//		ListIterator<EventMethodData> dataIt = dataList.listIterator();
//		while (dataIt.hasNext()) {
//			EventMethodData data = dataIt.next();
//			if (data.src.equals(src))
//				dataList.remove(data);
//		}
//	}
//
//	/**
//	 * Unregisters the methods that have been registered as listeners of the appropriate event type class in the eventClasses.
//	 *
//	 * @param src
//	 *            Source object
//	 * @param eventClasses
//	 *            Appropriate event types.
//	 */
//	@Override
//	public void unregister(Object src, Class<? extends Event>[] eventClasses) {
//		if (src == null)
//			return;
//
//		System.out.println("Unregistered: " + src.getClass().getCanonicalName());
//
//		for (Class<? extends Event> eventClass : eventClasses) {
//			List<EventMethodData> dataList = registeredListeners.get(eventClass);
//			if (dataList == null)
//				return;
//			ListIterator<EventMethodData> dataIt = dataList.listIterator();
//			while (dataIt.hasNext()) {
//				EventMethodData data = dataIt.next();
//				if (data.src.equals(src))
//					dataList.remove(data);
//			}
//		}
//	}
//
//	/**
//	 * Sends event to all of the registered listeners of the appropriate type.
//	 *
//	 * @param event
//	 *            Event to send.
//	 */
//	@Override
//	public void dispatch(Event event) {
//		System.out.println("DefaultEventBus.dispatch()");
//		Class<? extends Event> eventClass = event.getClass();
//		List<EventMethodData> dataList = registeredListeners.get(eventClass);
//		if (dataList == null)
//			return;
//
//		System.out.println("Dispatching.");
//
//		if (event instanceof EventStoppable) {
//			EventStoppable stoppable = (EventStoppable) event;
//			for (EventMethodData data : dataList) {
//				try {
//					data.method.invoke(data.src, event);
//				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//				}
//				if (stoppable.isStopped())
//					break;
//			}
//		} else {
//			for (EventMethodData data : dataList) {
//				try {
//					data.method.invoke(data.src, event);
//				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//				}
//			}
//		}
//	}
//
//	@Override
//	public void dispatch(Event[] events) {
//		System.out.println("DefaultEventBus.dispatch()");
//		if ((events == null) || (events.length == 0))
//			return;
//		System.out.println("Dispatching.");
//		for (Event e : events) {
//			dispatch(e);
//		}
//	}
//
//	private void putMap(Class<? extends Event> eventClasses, EventMethodData data) {
//		List<EventMethodData> dataList = registeredListeners.get(eventClasses);
//		if (dataList == null)
//			dataList = new CopyOnWriteArrayList<EventMethodData>();
//		dataList.add(data);
//		if (!registeredListeners.containsKey(eventClasses))
//			registeredListeners.put(eventClasses, dataList);
//		// prioritise(eventClasses);
//	}
//
//	private void prioritise(Class<? extends Event> eventClass) {
//		List<EventMethodData> dataList = registeredListeners.get(eventClass);
//		List<EventMethodData> newList = new CopyOnWriteArrayList<EventMethodData>();
//		if (dataList != null) {
//			for (EventPriority priority : EventPriority.values()) {
//				for (EventMethodData data : dataList) {
//					if (data.priority == priority)
//						newList.add(data);
//				}
//			}
//			registeredListeners.put(eventClass, newList);
//		}
//	}
//
//	/**
//	 * Checks whether the method is valid to be registered as a listener method.
//	 *
//	 * @param method
//	 *            Method to check.
//	 * @return Whether it is valid.
//	 */
//	private boolean isValid(Method method) {
//		if (method == null)
//			return false;
//		if (!method.isAnnotationPresent(EventTarget.class))
//			return false;
//		return (method.getParameterTypes().length == 1) && Event.class.isAssignableFrom(method.getParameterTypes()[0]);
//	}
// }