package org.mapleir.app.client;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public class SimpleApplicationContext extends AbstractApplicationContext {

	private final ApplicationClassSource app;
	
	public SimpleApplicationContext(ApplicationClassSource app) {
		this.app = app;
	}

	public static boolean isMainMethod(MethodNode m) {
		return Modifier.isPublic(m.node.access) && Modifier.isStatic(m.node.access) && m.getName().equals("main") && m.getDesc().equals("([Ljava/lang/String;)V");
	}
	
	private boolean isLibraryInheritedMethod(MethodNode m) {
		if(Modifier.isStatic(m.node.access) || m.getName().equals("<init>")) {
			return false;
		}
		
		ClassTree tree = app.getClassTree();
		
		// TODO: could probably optimise with dfs instead of getAll
		Collection<ClassNode> parents = tree.getAllParents(m.owner);
		for(ClassNode cn : parents) {
			if(app.isLibraryClass(cn.getName())) {
				for(MethodNode cnM : cn.getMethods()) {
					if(!Modifier.isStatic(cnM.node.access) && cnM.getName().equals(m.getName()) && cnM.getDesc().equals(m.getDesc())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	protected Set<MethodNode> computeEntryPoints() {
		Set<MethodNode> set = new HashSet<>();
		
		for(ClassNode cn : app.iterate()) {
			for(MethodNode m : cn.getMethods()) {
				if(isMainMethod(m) || m.getName().equals("<clinit>") || isLibraryInheritedMethod(m)) {
					set.add(m);
				}
			}
		}
		
		return set;
	}
}
