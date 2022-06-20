package org.topdank.eventbus;

import java.util.HashMap;
import java.util.Map;

import org.topdank.eventbus.events.ShutdownEvent;
import org.topdank.eventbus.impl.EventBuses;

public final class BusRegistry {

	private static final BusRegistry instance = new BusRegistry();

	static {
		EventBus bus = EventBuses.singleThreadBus();
		instance.add("global", bus);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				bus.dispatch(new ShutdownEvent());
			}
		});
	}

	private Map<String, EventBus> busMap;

	private BusRegistry() {
		busMap = new HashMap<String, EventBus>();
	}

	public void add(String name, EventBus bus) {
		busMap.put(name, bus);
	}

	public EventBus get(String name) {
		return busMap.get(name);
	}

	public EventBus getGlobalBus() {
		return get("global");
	}

	public static final BusRegistry getInstance() {
		return instance;
	}
}