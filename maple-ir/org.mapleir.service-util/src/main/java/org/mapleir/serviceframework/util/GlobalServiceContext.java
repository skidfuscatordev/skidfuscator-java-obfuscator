package org.mapleir.serviceframework.util;

import org.mapleir.serviceframework.api.IServiceContext;

final class GlobalServiceContext implements IServiceContext {
	
	@Override
	public String toString() {
		return "GlobalServiceContext";
	}
	
	@Override
	public boolean equals(Object o) {
		return o == this;
	}
}