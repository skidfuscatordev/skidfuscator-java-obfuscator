package dev.skidfuscator.obfuscator.resolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassHelper;
import org.mapleir.ir.TypeUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public class SkidHierarchyMethods {
	
	private static final int ANY_TYPES = 0;
	private static final int CONGRUENT_TYPES = 1;
	private static final int EXACT_TYPES = 2;
	private static final int LOOSELY_RELATED_TYPES = 3;
	private static final int VIRTUAL_METHOD = ~Modifier.STATIC & ~Modifier.ABSTRACT & ~Opcodes.ACC_BRIDGE;

	private final ApplicationClassSource app;
	
	public SkidHierarchyMethods(ApplicationClassSource app) {
		this.app = app;
	}
	
	public static boolean isBridge(int access) {
		return (access & Opcodes.ACC_BRIDGE) != 0;
	}
	
	private void debugCong(String expected, String actual) {
		Type eR = Type.getReturnType(expected);
		Type aR = Type.getReturnType(actual);
		
		System.err.println("eR: " + eR);
		System.err.println("aR: " + aR);
		System.err.println("eq: " + (eR == aR));

		ClassNode eCn = app.findClassNode(eR.getInternalName());
		ClassNode aCn = app.findClassNode(aR.getInternalName());
		
		System.err.println("eCn: " + eCn.getName());
		System.err.println("aCn: " + aCn.getName());
		System.err.println(app.getClassTree().getAllParents(aCn));
		System.err.println("eCn parent of aCn?: " + app.getClassTree().getAllParents(aCn).contains(eCn));
		System.err.println("aCn child of eCn?: " + app.getClassTree().getAllChildren(eCn).contains(aCn));
	}
	
	/**
	 * returns true if::
	 *  both types are primitives of the same
	 *   race.
	 * or
	 *  the types are both objects.
	 * or
	 *  they are both array types and have the
	 *  same number of dimensions and have
	 *  loosely or strongly (T I G H T L Y)
	 *  related element types.
	 */
	private boolean areTypesLooselyRelated(Type t1, Type t2) {
		/* getSort() returns V, Z, C, B, I, F, J, D, array or object */
		if(t1.getSort() == t2.getSort()) {
			if(t1.getSort() == Type.ARRAY) {
				/* both arrays */
				
				if(t1.getDimensions() != t2.getDimensions()) {
					return false;
				}
				
				return areTypesLooselyRelated(t1.getElementType(), t2.getElementType());
			}
			
			/* either strongly related (prims) or
			 * loosely (object types) */
			return true;
		} else {
			/* no chance m8 */
			return false;
		}
	}
	
	/**
	 * Two types are congruent if they are primitive and the same, or if one is a subclass of another.
	 * @param a type A
	 * @param b type B
	 * @return true if type A and B are congruent
	 */
	private boolean areTypesCongruent(Type a, Type b) {
		if (a.equals(b)) {
			return true;
		}
		
		boolean eArr = a.getSort() == Type.ARRAY;
		boolean aArr = b.getSort() == Type.ARRAY;
		if(eArr != aArr) {
			return false;
		}
		
		if(eArr) {
			a = a.getElementType();
			b = b.getElementType();
		}
		
		if(TypeUtils.isPrimitive(a) || TypeUtils.isPrimitive(b)) {
			return false;
		}
		if(a == Type.VOID_TYPE || b == Type.VOID_TYPE) {
			return false;
		}
		
		ClassNode cnA = app.findClassNode(a.getInternalName());
		ClassNode cnB = app.findClassNode(b.getInternalName());
		
		ClassTree tree = app.getClassTree();
		return tree.getAllParents(cnB).contains(cnA) ||
               tree.getAllParents(cnA).contains(cnB);
	}
	
	/**
	 * Finds methods in cn matching name and desc.
	 * @param cn ClassNode
	 * @param name name of method
	 * @param desc method descriptor
	 * @param returnTypes One of ANY_TYPE, CONGRUENT_TYPES, EXACT_TYPES, or LOOSELY_RELATED_TYPES
	 * @param allowedMask Mask of allowed attributes for modifiers; bit 1 = allowed, 0 = not allowed
	 * @param requiredMask Mask of required attributes for modifiers; bit 1 = allowed, 0 = not allowed
	 * @return Set of methods matching specifications
	 */
	private Set<MethodNode> findMethods(ClassNode cn, String name, String desc, int returnTypes, int allowedMask, int requiredMask) {
		allowedMask |= requiredMask;
		Set<MethodNode> findM = new HashSet<>();
		
		Type[] expectedParams = Type.getArgumentTypes(desc);
		
		for(MethodNode m : cn.getMethods()) {
			// no bits set in m.access that aren't in allowedMask
			// no bits unset in m.access that are in requiredMask
			if(((m.node.access ^ allowedMask) & m.node.access) == 0 && ((m.node.access ^ requiredMask) & requiredMask) == 0) {
				if (!Modifier.isStatic(allowedMask) && Modifier.isStatic(m.node.access))
					throw new IllegalStateException("B0i");
				if (!Modifier.isAbstract(allowedMask) && Modifier.isAbstract(m.node.access))
					throw new IllegalStateException("B0i");
				if (!isBridge(allowedMask) && isBridge(m.node.access))
					throw new IllegalStateException("B0i");
				if (Modifier.isStatic(requiredMask) && !Modifier.isStatic(m.node.access))
					throw new IllegalStateException("B0i");

				if (!m.getName().equals(name) || !Arrays.equals(expectedParams, Type.getArgumentTypes(m.getDesc()))) {
					continue;
				}

				switch(returnTypes) {
					case ANY_TYPES:
						break;
					case CONGRUENT_TYPES:
						if (!areTypesCongruent(Type.getReturnType(desc), Type.getReturnType(m.getDesc()))) {
							continue;
						}
						break;
					case EXACT_TYPES:
						if (!desc.equals(m.getDesc())) {
							continue;
						}
						break;
					case LOOSELY_RELATED_TYPES:
						if(!areTypesLooselyRelated(Type.getReturnType(desc), Type.getReturnType(m.getDesc()))) {
							continue;
						}
						break;
				}
				
				// sanity check
				if (returnTypes == EXACT_TYPES && !isBridge(allowedMask) && !findM.isEmpty()) {
					System.err.println("==findM==");
					debugCong(desc, findM.iterator().next().getDesc());
					System.err.println("==m==");
					debugCong(desc, m.getDesc());
					
					{
						byte[] bs = ClassHelper.toByteArray(cn);
						
						try {
							FileOutputStream fos = new FileOutputStream(new File("out/broken.class"));
							fos.write(bs);
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
					
					throw new IllegalStateException(String.format("%s contains %s(br=%b) and %s(br=%b)", cn.getName(), findM, isBridge(findM.iterator().next().node.access), m, isBridge(m.node.access)));
				}
				findM.add(m);
			}
		}
		
		return findM;
	}
	
	public Set<MethodNode> getHierarchyMethodChain(ClassNode cn, String name, String desc, boolean exact) {
		ClassTree structures = app.getClassTree();
		
		Set<MethodNode> foundMethods = new HashSet<>();
		Collection<ClassNode> toSearch = structures.getAllBranches(cn);
		for (ClassNode viable : toSearch) {
			foundMethods.addAll(findMethods(viable, name, desc, exact? EXACT_TYPES : CONGRUENT_TYPES, VIRTUAL_METHOD | Modifier.ABSTRACT | Opcodes.ACC_BRIDGE, 0));
		}

		for (MethodNode foundMethod : new HashSet<>(foundMethods)) {
			for (ClassNode allChild : structures.getAllChildren(foundMethod.owner)) {
				foundMethods.addAll(findMethods(allChild, name, desc, exact? EXACT_TYPES : CONGRUENT_TYPES, VIRTUAL_METHOD | Modifier.ABSTRACT | Opcodes.ACC_BRIDGE, 0));
			}
		}
		return foundMethods;
	}
}
