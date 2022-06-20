package org.topdank.eventbus.impl;
//package org.zbot.topdank.eventbus.impl;
//
//import java.lang.reflect.InvocationTargetException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReadWriteLock;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//
//import org.zbot.topdank.eventbus.Event;
//import org.zbot.topdank.eventbus.EventMethodData;
//import org.zbot.topdank.eventbus.EventStoppable;
//
//public class ConcurrentEventBus extends DefaultEventBus {
//
//	private final Lock readLock /* ,writeLock */;
//
//	protected ConcurrentEventBus() {
//		super();
//		ReadWriteLock lock = new ReentrantReadWriteLock(true);
//		readLock = lock.readLock();
//		// writeLock = lock.writeLock();
//	}
//
//	@Override
//	public void dispatch(Event event) {
//		Class<? extends Event> eventClass = event.getClass();
//		List<EventMethodData> dataList = copyFindListenerDataTargets(eventClass);
//
//		if (dataList == null)
//			return;
//
//		// System.out.println("Fired " +
//		// event.getClass().getSimpleName());
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
//	private List<EventMethodData> copyFindListenerDataTargets(Class<?> eventClass) {
//		List<EventMethodData> data;
//		readLock.lock();
//		try {
//			if (registeredListeners.size() == 0) {
//				return null;
//			} else {
//				data = new ArrayList<EventMethodData>();
//				List<EventMethodData> data1 = registeredListeners.get(eventClass);
//				if ((data1 == null) || (data1.size() == 0)) {
//					return null;
//				}
//				data.addAll(data1);
//				return data;
//			}
//		} finally {
//			readLock.unlock();
//		}
//	}
// }