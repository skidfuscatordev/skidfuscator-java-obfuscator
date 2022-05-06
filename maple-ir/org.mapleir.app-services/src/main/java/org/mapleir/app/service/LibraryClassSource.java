package org.mapleir.app.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;

public class LibraryClassSource extends ClassSource {

	protected final ApplicationClassSource parent;
	
	protected LibraryClassSource(ApplicationClassSource parent) {
		this(parent, new HashMap<>());
	}
		
	public LibraryClassSource(ApplicationClassSource parent, Collection<ClassNode> classes) {
		this(parent, ClassHelper.convertToMap(classes));
	}
	
	public LibraryClassSource(ApplicationClassSource parent, Map<String, ClassNode> nodeMap) {
		super(nodeMap);
		
		this.parent = parent;
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
}
