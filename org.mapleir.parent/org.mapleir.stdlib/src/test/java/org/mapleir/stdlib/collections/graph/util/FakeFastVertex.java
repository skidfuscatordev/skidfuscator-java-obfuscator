package org.mapleir.stdlib.collections.graph.util;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class FakeFastVertex implements FastGraphVertex {
	private final String id;
	
	public FakeFastVertex(int id) {
		this.id = String.valueOf(id);
	}

	public FakeFastVertex(String id) {
		this.id = id;
	}

	@Override
	public int getNumericId() {
		return id.hashCode();
	}

	@Override
	public String getDisplayName() {
		return id;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}
