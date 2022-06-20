package org.mapleir.app.client;

import java.util.Set;

import org.mapleir.asm.MethodNode;

public interface ApplicationContext {

	Set<MethodNode> getEntryPoints();
}
