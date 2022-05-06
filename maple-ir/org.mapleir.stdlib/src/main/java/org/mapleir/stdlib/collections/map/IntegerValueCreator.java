package org.mapleir.stdlib.collections.map;

import java.util.concurrent.atomic.AtomicInteger;

public class IntegerValueCreator implements ValueCreator<AtomicInteger> {

	@Override
	public AtomicInteger create() {
		return new AtomicInteger();
	}
}