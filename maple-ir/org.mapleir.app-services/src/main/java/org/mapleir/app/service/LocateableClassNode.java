package org.mapleir.app.service;

import org.mapleir.asm.ClassNode;

public class LocateableClassNode {

	public final ClassSource source;
	public final ClassNode node;
	public final boolean isVMKlass;
	
	public LocateableClassNode(ClassSource source, ClassNode node, boolean isVMKlass) {
		this.source = source;
		this.node = node;
		this.isVMKlass = isVMKlass;
	}
	
	@Override
	public String toString() {
		return String.format("%s from %s%s", node.getName(), source, isVMKlass ? "(vm)" : "");
	}
}
