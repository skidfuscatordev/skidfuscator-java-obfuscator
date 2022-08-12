package org.mapleir.app.client;

import java.util.Set;

import org.mapleir.asm.MethodNode;

public abstract class AbstractApplicationContext implements ApplicationContext {
	
	private Set<MethodNode> impl;
	
	@Override
	public final Set<MethodNode> getEntryPoints() {
		if(impl == null) {
			impl = computeEntryPoints();
		}
		return impl;
	}

	protected abstract Set<MethodNode> computeEntryPoints();
}
