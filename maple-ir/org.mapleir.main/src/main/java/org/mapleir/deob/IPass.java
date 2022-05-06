package org.mapleir.deob;

public interface IPass {
	
	default boolean is(Class<? extends IPass> clz) {
		return getClass() == clz;
	}
	
	default boolean is(String id) {
		return getId().equals(id);
	}
	
	default String getId() {
		return getClass().getSimpleName();
	}

	PassResult accept(PassContext cxt);
}