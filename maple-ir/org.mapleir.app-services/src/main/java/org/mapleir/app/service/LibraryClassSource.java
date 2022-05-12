package org.mapleir.app.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;

public class LibraryClassSource extends ClassSource {

	protected final ApplicationClassSource parent;
	protected final int priority;

	protected LibraryClassSource(ApplicationClassSource parent) {
		this(parent, 0, new HashMap<>());
	}

	public LibraryClassSource(ApplicationClassSource parent, int priority) {
		this(parent, priority, new HashMap<>());
	}

	public LibraryClassSource(ApplicationClassSource parent, Collection<ClassNode> classes) {
		this(parent, 0, ClassHelper.convertToMap(classes));
	}
	
	public LibraryClassSource(ApplicationClassSource parent, int priority, Map<String, ClassNode> nodeMap) {
		super(nodeMap);
		
		this.parent = parent;
		this.priority = priority;
	}
	
	/* public lookup method, polls parent first (which can
	 * call it's children to look for the */
	@Override
	public LocateableClassNode findClass(String name) {
		if(name == null) {
			return null;
		}
		
		if(parent != null) {
			return parent.findClass(name);
		} else {
			throwNoParent();
			return null;
		}
	}
	
	public boolean isIterable() {
		return true;
	}

	public int getPriority() {
		return priority;
	}
}
