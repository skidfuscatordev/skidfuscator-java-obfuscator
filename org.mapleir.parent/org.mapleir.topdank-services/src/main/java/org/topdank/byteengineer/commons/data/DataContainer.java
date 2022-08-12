package org.topdank.byteengineer.commons.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public abstract class DataContainer<T> extends ArrayList<T> {

	private static final long serialVersionUID = -9022506488647444546L;

	public DataContainer() {
		this(16);
	}

	public DataContainer(int cap) {
		super(cap);
	}

	public DataContainer(Collection<T> data) {
		addAll(data);
	}

	public abstract Map<String, T> namedMap();
}