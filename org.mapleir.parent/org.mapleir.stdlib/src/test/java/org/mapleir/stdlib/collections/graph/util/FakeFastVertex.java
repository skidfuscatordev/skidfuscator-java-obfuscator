package org.mapleir.stdlib.collections.graph.util;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class FakeFastVertex implements FastGraphVertex {
	private final int id;
	
	public FakeFastVertex(int id) {
		this.id = id;
	}

	@Override
	public int getNumericId() {
		return id;
	}

	@Override
	public String getDisplayName() {
		return String.valueOf(id);
	}
}
