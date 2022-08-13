package org.mapleir.app.service;

import java.util.*;

import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;

public class LibraryClassSource extends ClassSource {

	protected ApplicationClassSource parent;
	protected final int priority;

	public LibraryClassSource(Collection<ClassNode> classes, ApplicationClassSource parent, int priority) {
		super(classes);
		this.parent = parent;
		this.priority = priority;
	}

	public LibraryClassSource(Map<String, ClassNode> nodeMap, ApplicationClassSource parent, int priority) {
		super(nodeMap);
		this.parent = parent;
		this.priority = priority;
	}

	public LibraryClassSource(ApplicationClassSource parent, int priority) {
		this(Collections.emptySet(), parent, priority);
	}

	/* public lookup method, polls parent first (which can
	 * call its children to look for the */
	@Override
	public LocateableClassNode findClass(String name) {
		if(name == null) {
			return null;
		}
		
		if(parent != null) {
			return parent.findClass0(name);
		} else {
			throwNoParent();
			return null;
		}
	}

	@Override
	protected LocateableClassNode findClass0(String name) {
		if(name == null) {
			return null;
		}

		if(parent != null) {
			return parent.findClass0(name);
		} else {
			throwNoParent();
			return null;
		}
	}

	@Override
	public boolean contains(String name) {
		return parent.contains(name);
	}

	public boolean isIterable() {
		return true;
	}

	public int getPriority() {
		return priority;
	}
}
