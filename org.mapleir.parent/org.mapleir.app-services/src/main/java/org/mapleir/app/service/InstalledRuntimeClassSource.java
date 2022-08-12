package org.mapleir.app.service;

import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.mapleir.asm.ClassHelper;
import org.objectweb.asm.ClassReader;
import org.mapleir.asm.ClassNode;

public class InstalledRuntimeClassSource extends LibraryClassSource {
	protected final Logger LOGGER = Logger.getLogger(InstalledRuntimeClassSource.class);

	private final HashSet<String> notContains;
	
	public InstalledRuntimeClassSource(ApplicationClassSource parent) {
		super(parent, 0);
		notContains = new HashSet<>();
	}
	
	@Override
	public boolean contains(String name) {
		if(super.contains(name)) {
			return true;
		} else if(!notContains.contains(name)) {
			return __resolve(name) != null;
		} else {
			return false;
		}
	}

	@Override
	protected LocateableClassNode findClass0(String name) {
		/* check the cache first. */
		LocateableClassNode node = super.findClass0(name);
		if(node != null) {
			return node;
		}
		
		return __resolve(name);
	}
	
	/* (very) internal class loading method. doesn't
	 * poll cache at all. */
	private LocateableClassNode __resolve(String name) {
		if(name.startsWith("[")) {
			/* calling Object methods. (clone() etc)
			 * that we haven't already resolved.
			 * 
			 * Cache it so that contains() can
			 * quick check whether we have it
			 * and the next call to findClass0
			 * can quickly resolve it too. */
			LocateableClassNode node = findClass0("java/lang/Object");
			nodeMap.put(name, node.node);
			return node;
		}

		/* try to resolve the class from the runtime. */
		try {
			ClassNode cn = ClassHelper.create(name, ClassReader.SKIP_CODE);
			/* cache it. */
			nodeMap.put(cn.getName(), cn);
			
			ClassTree tree = parent._getClassTree();
			if(tree == null) {
				if(!cn.getName().equals("java/lang/Object")) {
					LOGGER.error(String.format("Tried to load %s before initialisation", cn));
					throw new IllegalStateException("Only Object may be loaded during tree initialisation.");
				}
			} else {
				if(!tree.containsVertex(cn)) {
					tree.addVertex(cn);
				}
			}
			
			LocateableClassNode node = new LocateableClassNode(this, cn, true);
			return node;
		} catch(IOException e) {
			LOGGER.error(String.format("Could not load class from calling classloader: %s", name));
			LOGGER.error(e);
			notContains.add(name);
			return null;
		}
	}
	
	@Override
	public boolean isIterable() {
		return false;
	}
	
	@Override
	public String toString() {
		return "JRE " + System.getProperty("java.version");
	}
}
