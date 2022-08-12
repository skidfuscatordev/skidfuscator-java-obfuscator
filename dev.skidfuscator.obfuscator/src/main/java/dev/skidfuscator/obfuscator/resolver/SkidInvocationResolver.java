package dev.skidfuscator.obfuscator.resolver;

import org.apache.log4j.Logger;
import org.mapleir.HierarchyMethods;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.InvocationResolver;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.objectweb.asm.Opcodes;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.FieldNode;
import org.mapleir.asm.MethodNode;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

public class SkidInvocationResolver implements InvocationResolver {
	
	private static final Logger LOGGER = Logger.getLogger(SkidInvocationResolver.class);
	
	/* trick for no IDE warning */
	private static int __debug_level() { return 0; }
	private static final int debugLevel = __debug_level();
	private static final boolean disallowMultiMS = false;
	private static final boolean allowMissingClasses = true;
	
	private final ApplicationClassSource app;
	
	private final Map<ClassNode, Map<Selector, MethodNode>> concreteVTables = new HashMap<>();
	private final Map<ClassNode, Map<Selector, MethodNode>> abstractVTables = new HashMap<>();
	
	// TODO: migrate to cleaner system
	private final SkidHierarchyMethods hierarchyMethodsHelper;
	
	public SkidInvocationResolver(ApplicationClassSource app) {
		this.app = app;
		hierarchyMethodsHelper = new SkidHierarchyMethods(app);
		
		//computeVTables();
		
		LOGGER.info(String.format("built vtables for %s classes", concreteVTables.size()));
	}
	
	protected boolean hasVisited(ClassNode c) {
		return concreteVTables.containsKey(c) && abstractVTables.containsKey(c);
	}
	
	public void computeVTables() {
		for(ClassNode c : app.getClassTree().vertices()) {
			if(app.getClassTree().getChildren(c).size() == 0) {
				// also implies getAllChildren.size() == 0
				computeVTable(c);
			}
		}
	}
	
	static class CompFrame {
		final ClassNode c;
		final Map<Selector, MethodNode> thisMethodSet;
		final Map<Selector, MethodNode> thisAbstractSet;
		final Map<Selector, MethodNode> globalCVT;
		final Map<Selector, MethodNode> globalAVT;
		final NullPermeableHashMap<Selector, Set<MethodNode>> mergeMap;
		
		public CompFrame(ClassNode c) {
			this.c = c;
			
			thisMethodSet = new HashMap<>();
			thisAbstractSet = new HashMap<>();
			globalAVT = new HashMap<>();
			globalCVT = new HashMap<>();
			mergeMap = new NullPermeableHashMap<>(HashSet::new);
		}
	}
	
	private void computeVTable(ClassNode c) {
		if(c == null) {
			throw new NullPointerException();
		}
		
		/* only visit each class once */
		if(hasVisited(c)) {
			return;
		}
		
		/* ensure parents loaded */
		ClassNode superKlass = null;
		/* if the super class is null it means we're at object and so
		 * we don't even have to consider interfaces: as we stop
		 * here */
		if(c.node.superName != null) {
			computeVTable(superKlass = app.findClassNode(c.node.superName));
			for(String i : c.node.interfaces) {
				computeVTable(app.findClassNode(i));
			}
		}

		/* the general plan of attack here is:
		 *  1. add our own methods.
		 *  2. merge without our super class methods.
		 *  3. merge and resolve with our interfaces. */
		
		/* we build our local tables for the current class. it's
		 * important to keep these separate from what will become the
		 * completely hierarchy sensitive lookup tables later and so
		 * technically should be immutable after the loop but we can
		 * ignore this as long as we do not modify them later. */
		Map<Selector, MethodNode> thisMethodSet = new HashMap<>();
		Map<Selector, MethodNode> thisAbstractSet = new HashMap<>();
		
		for(MethodNode m : c.getMethods()) {
			/* we can easily resolve these as they come
			 * so we don't even need them in the table. */
			if(Modifier.isStatic(m.node.access)) {
				continue;
			}
			
			/* immutable lookup key */
			Selector s = new Selector(m.getName(), m.getDesc());
			
			/* store our local declarations. */
			if(Modifier.isAbstract(m.node.access)) {
				putOrThrow(thisAbstractSet, s, m);
			} else {
				putOrThrow(thisMethodSet, s, m);
			}
		}
		
		if(debugLevel >= 2) {
			LOGGER.debug("Class: " + c);
			LOGGER.debug("  super: " + c.node.superName);
			LOGGER.debug("  interfaces: " + c.node.interfaces.toString());
			LOGGER.debug(" implSet: ");
			print(thisMethodSet);
			LOGGER.debug(" absSet: " );
			print(thisAbstractSet);
		}
		
		/* Store the local results as the global as in the case of
		 * java/lang/Object we don't have a super class and so the
		 * ensuing analysis (below) is never executed. It's important
		 * to note that we shouldn't be looking in the global vtable
		 * for a given class while processing that class; the local
		 * maps/vtable values are used but not the field
		 * references. */
		concreteVTables.put(c, thisMethodSet);
		abstractVTables.put(c, thisAbstractSet);
		
		/* now we consider the super class which we have previously
		 * completely resolved. now we have to propagate information
		 * from the parent to the child. the following rules are
		 * defined:
		 * 
		 * 1. abstract definitions in the child class always kill the
		 *    parent propagated definition. this is presumably also true
		 *    when the definition is inherited from a default interface
		 *    but we handle this after. 
		 * 
		 * 2. a concrete definition in the
		 *    child will always override the parent propagated
		 *    definition, this is again, true if the parent propagated
		 *    definitions come from default interfaces but we don't deal
		 *    with that here. */
		
		if(superKlass != null) {
			if(!hasVisited(superKlass)) {
				throw new IllegalStateException(String.format("Parent of %s, %s is not initialised", c, superKlass));
			}

			Map<Selector, MethodNode> globalCVT = new HashMap<>();
			Map<Selector, MethodNode> globalAVT = new HashMap<>();
			/* inherit all super class methods */
			globalCVT.putAll(concreteVTables.get(superKlass));
			globalAVT.putAll(abstractVTables.get(superKlass));
			
			assertIntersection(thisAbstractSet.entrySet(), thisMethodSet.entrySet(), Collections.emptySet());
			/* (1) and (2) 
			 * NOTE: thisAbstractSet and thisMethodSet should
			 *       intersect to give the null set. */
			for(Selector s : thisAbstractSet.keySet()) {
				globalCVT.remove(s);
			}
			for(Selector s : thisMethodSet.keySet()) {
				globalAVT.remove(s);
			}
			
			/* we shouldn't ever get merge errors from considering the
			 * current class with it's super. (this can happen with
			 * interfaces, however) */
			assertIntersection(globalAVT.entrySet(), globalCVT.entrySet(), Collections.emptySet());
			
			/* add our own declarations to the tables. this could possibly
			 * override methods from the super class and we're happy
			 * about this. */
			globalCVT.putAll(thisMethodSet);
			globalAVT.putAll(thisAbstractSet);
			assertIntersection(globalAVT.entrySet(), globalCVT.entrySet(), Collections.emptySet());
			
			concreteVTables.put(c, globalCVT);
			abstractVTables.put(c, globalAVT);
			
			if(debugLevel >= 3) {
				LOGGER.debug(" globalCVT: ");
				print(globalCVT);
				LOGGER.debug(" globalAVT: ");
				print(globalAVT);
			}

			NullPermeableHashMap<Selector, Set<MethodNode>> mergeMap = new NullPermeableHashMap<>(HashSet::new);
			
			if(debugLevel >= 2) {
				LOGGER.debug(" process interfaces:");
			}
			
			for(String i : c.node.interfaces) {
				if(debugLevel >= 2) {
					LOGGER.debug("  " + i);
				}
				ClassNode interfaceKlass = app.findClassNode(i);
				/* An abstract interface method cannot kill
				 * a concrete class implementation of the method
				 * from the super class. However, if the reaching
				 * definition is a default implementation from
				 * another interface which is a superclass of the
				 * current interface, then the reaching definition
				 * in the current interface is an abstract method
				 * and so that is propagated into the implementor.
				 * 
				 * If for example the super interface defines the
				 * abstract method and the sub interface has a
				 * default implementation and we implement the 
				 * default interface, the default method will become
				 * the reaching definition for the current class,
				 * not the abstract one.
				 * 
				 * If a class that implements a default method
				 * already has a reaching definition in the parent,
				 * the subinterface default method takes priority.*/
				
				add(concreteVTables.get(interfaceKlass), mergeMap);
				add(abstractVTables.get(interfaceKlass), mergeMap);
			}

			if(debugLevel >= 3) {
				LOGGER.debug("   mergeMap:");
				printMap(mergeMap);
			}
			
			for(Entry<Selector, Set<MethodNode>> e : mergeMap.entrySet()) {
				Selector selector = e.getKey();
				Set<MethodNode> conflictingMethods = e.getValue();
				
				MethodNode resolve;
				
				if(conflictingMethods.size() > 1) {
					/* conflict, C must explicitly declare m. */
					
					if(debugLevel >= 2) {
						LOGGER.debug("    conflicts: " + conflictingMethods.size());
						LOGGER.debug("      " + selector);
						for(MethodNode m : conflictingMethods) {
							LOGGER.debug("      ^" + m);
						}
					}
					
					/* if we have any sort of declaration, we can easily say that
					 * that is the target without having to check the hierarchy
					 * and globalCVT rules. 
					 * 
					 * 05/10/17: globalCVT should contain super defs as well or ours
					 *           so surely we use this as it doesn't contain any
					 *           abstracts?*/
					if(globalCVT.containsKey(selector)) {
						resolve = globalCVT.get(selector);
						
						if(debugLevel >= 2) {
							LOGGER.debug("    (1)resolved to " + resolve);
						}
					} else if(thisAbstractSet.containsKey(selector)) {
						resolve = thisAbstractSet.get(selector);
						
						if(debugLevel >= 2) {
							LOGGER.debug("    (2)resolved to " + resolve);
						}
					} else {
						/* here is where it gets tricky. */
						
						/* 05/10/17: weird bug? example:
						 *    javax/swing/LayoutComparator implements Comparable
						 *    Comparable extends Object by OUR own definition.
						 *    Comparable reabstracts equals(Object) but LayoutComparator
						 *    extends Object implicitly, so the Object definition is
						 *    reaching unless it is redefined in LayoutComparator.
						 *    Note that you can't have default methods for Object methods.
						 *    
						 *    So do we check for it here or inherit Objects methods before we
						 *    do anything else?
						 *    solution: use global vtable merge interface merges above?
						 */
						
						Collection<MethodNode> contenders = getMaximallySpecific(conflictingMethods);
						if(contenders.isEmpty()) {
							throw new IllegalStateException();
						}
						
						if(contenders.size() == 1) {
							/* design decision: if there is only 1 contender but multiple
							 * conflicting methods, all of the conflicting methods are
							 * defaults, then we have a clear target method, but if any of
							 * them are abstract, we are actually propagating a
							 * reabstraction through this class, so we can insert a
							 * miranda. */
							
							boolean anyAbstract = false;
							for(MethodNode m : contenders) {
								anyAbstract |= Modifier.isAbstract(m.node.access);
							}
							
							if(anyAbstract) {
								/* valid: miranda (reabstraction) */
								resolve = null;
							} else {
								resolve = contenders.iterator().next();
							}
							
							
							if(debugLevel >= 2) {
								LOGGER.debug("    (3)resolved to " + resolve);
							}
						} else {
							/* if there are multiple maximally specific declarations of
							 * the method, we really do need a declaration in C if not all
							 * of the contenders are abstract (if not all of the
							 * contenders are abstract, we need a declaration in C to fix
							 * the disambiguity). if C is abstract and all of the
							 * contenders are abstract, we don't need to explicitly
							 * declare the method because it can be implied but we can
							 * insert a miranda to create a link in the vtable. */

							if(Modifier.isAbstract(c.node.access)) {
								boolean allAbstract = true;
								for(MethodNode m : contenders) {
									allAbstract &= Modifier.isAbstract(m.node.access);
								}
								
								if(allAbstract) {
									/* valid: miranda */
									resolve = null;
								} else {
									/* require a declaration but we don't
									 * have one -> die. */
									throw new IllegalStateException(String.format("Method %s is ambiguous in %s, candidates:%s", selector, c, conflictingMethods));
								}
							} else {
								/* require a declaration but we don't
								 * have one -> die. */
								throw new IllegalStateException(String.format("Method %s is ambiguous in %s, candidates:%s", selector, c, conflictingMethods));
							}
						}
					}
				} else {
					/* no conflicts, so we can just choose the resolve method as
					 * the target method from one of the tables. */
					if(globalCVT.containsKey(selector)) {
						resolve = globalCVT.get(selector);
					} else if(thisAbstractSet.containsKey(selector)) {
						/* thisAbstractSet instead of the global one because
						 * these conflicts would make things confusing if
						 * the global table was constantly changing. */
						resolve = thisAbstractSet.get(selector);
					} else {
						/* no conflict but the current class just does not implement
						 * the method itself. we could either create a miranda here or
						 * reference to method. we will do the latter. */
						resolve = conflictingMethods.iterator().next(); // size == 1
					}
				}
				
				/* delete from both so we can do a clean add after. */
				globalAVT.remove(selector);
				globalCVT.remove(selector);
				
				if(resolve == null && !Modifier.isAbstract(c.node.access)) {
					throw new IllegalStateException(String.format("Miranda %s in non abstract class %s", conflictingMethods, c));
				}
				
				/* if resolve is null, there is resolveable
				 * definition (miranda case), so we generate
				 * it and add it. */
				
				if(resolve == null) {
					// TODO: sigs?
					resolve = new MethodNode(new org.objectweb.asm.tree.MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, selector.name, selector.desc, null, getExceptionClasses(conflictingMethods)), c);
					c.addMethod(resolve);
					if(debugLevel >= 2) {
						LOGGER.debug("  generated miranda " + resolve);
					}
				}
				
				if(Modifier.isAbstract(resolve.node.access)) {
					globalAVT.put(selector, resolve);
				} else {
					globalCVT.put(selector, resolve);
				}
			}
			
			assertIntersection(concreteVTables.get(c).entrySet(), abstractVTables.get(c).entrySet(), Collections.emptySet());
//			validateTables();
			
			if(debugLevel >= 2) {
				LOGGER.debug(" cvtable: ");
				print(concreteVTables.get(c));
				LOGGER.debug(" avtable: " );
				print(abstractVTables.get(c));
			}
		}
	}
	
	private <K, V> void putOrThrow(Map<K, V> map, K k, V v) {
		if(map.containsKey(k)) {
			throw new IllegalStateException(String.format("contention: prev: %s vs cur: %s", map.get(k), v));
		} else {
			map.put(k, v);
		}
	}
	
	private void validateTables() {
		for(Map<Selector, MethodNode> map : concreteVTables.values()) {
			for(MethodNode m : map.values()) {
				if(Modifier.isAbstract(m.node.access)) {
					throw new IllegalStateException();
				}
			}
		}

		for(Entry<ClassNode, Map<Selector, MethodNode>> e : abstractVTables.entrySet()) {
			Map<Selector, MethodNode> map = e.getValue();
			if(map.size() > 0) {
				if(!Modifier.isAbstract(e.getKey().node.access)) {
					throw new IllegalStateException();
				}
			}
			for(MethodNode m : map.values()) {
				if(!Modifier.isAbstract(m.node.access)) {
					throw new IllegalStateException();
				}
			}
		}
	}
	
	private Collection<MethodNode> getMaximallySpecific(Set<MethodNode> set) {
		// TODO: testing needed
		if (set.size() != 2) {
			throw new RuntimeException();
		}

		List<MethodNode> max = new ArrayList<>();
		outer: for (MethodNode m : set) {
			ClassNode c = m.owner;

			for (Iterator<MethodNode> maxiter = max.iterator(); maxiter.hasNext();) {
				MethodNode maxMethod = maxiter.next();
				ClassNode maxKlass = maxMethod.owner;
				
				boolean either = false;

				if (either |= isSuperOf(maxKlass, c)) {
					if(disallowMultiMS) {
						either |= isSuperOf(c, maxKlass);
						if (!either) {
							throw new RuntimeException(
									String.format("Different branches: %s and %s from %s", m, maxMethod, set));
						}
					}
					continue outer;
				}

				if (either |= isSuperOf(c, maxKlass)) {
					maxiter.remove();
				}

				if(disallowMultiMS) {
					if (!either) {
						throw new RuntimeException(
								String.format("Different branches: %s and %s from %s", m, maxMethod, set));
					}
				}
			}

			max.add(m);
		}

		if(disallowMultiMS) {
			if (max.size() != 1) {
				throw new RuntimeException(set.toString() + " > " + max.toString() + " investigate");
			}
		}
		
		/* it is possible that a class may have multiple maximally
		 * specific super interface methods. we need to find
		 * production examples of this because it is very fucky. */
		return max;
	}
	
	private String[] getExceptionClasses(Collection<MethodNode> col) {
		if(col.size() == 0) {
			throw new UnsupportedOperationException();
		} else if(col.size() == 1) {
			return col.iterator().next().node.exceptions.toArray(new String[0]);
		} else {
			Set<String> set = new HashSet<>();
			Iterator<MethodNode> it = col.iterator();
			set.addAll(it.next().node.exceptions);
			
			while(it.hasNext()) {
				List<String> lst = it.next().node.exceptions;
				
				/*if(lst.size() != set.size() || (lst.size() != 0 && !lst.containsAll(set))) {
					throw new IllegalStateException(String.format("set: %s, lst: %s for %s", set, lst, col));
				}*/
				set.addAll(lst);
			}
			
			return set.toArray(new String[0]);
		}
	}
	
	private boolean isSuperOf(ClassNode subKlass, ClassNode superKlass) {
		// return true iff n2 is a superclass or interface of n1
		return app.getClassTree().getAllParents(subKlass).contains(superKlass);
	}
	
	private void add(Map<Selector, MethodNode> map,
			NullPermeableHashMap<Selector, Set<MethodNode>> conflicts) {
		
		for(Entry<Selector, MethodNode> e : map.entrySet()) {
			conflicts.getNonNull(e.getKey()).add(e.getValue());
		}
	}
	
	public MethodNode resolve(ClassNode receiver, String name, String desc, boolean strict) {
		/*if(strict && receiver.isAbstract()) {
			throw new UnsupportedOperationException(String.format("Tried to call method on abstract receiver: %s.%s %s", receiver, name, desc));
		}*/
		Selector selector = new Selector(name, desc);
		
		if(!hasVisited(receiver)) {
			throw new UnsupportedOperationException(String.format("No table for %s", receiver));
		} else {
			Map<Selector, MethodNode> cvtable = concreteVTables.get(receiver);
			Map<Selector, MethodNode> avtable = abstractVTables.get(receiver);
			
			MethodNode cm = cvtable.get(selector);
			MethodNode am = avtable.get(selector);
			
			if(cm == null && am == null) {
				if(strict) {
					throw new NoSuchMethodError(receiver.getName() + "." + name + desc);
				}
			} else if(cm != null) {
				return cm;
			} else if(am != null) {
				if(strict) {
					throw new AbstractMethodError(receiver.getName() + "." + name + desc);
				} else {
					return am;
				}
			} else {
				if(strict) {
					throw new IllegalStateException(String.format("Multiple target sites %s and %s", cm, am));
				}
			}
		}
		
		return null;
	}

	@Override
	public MethodNode resolveStaticCall(String owner, String name, String desc) {
		ClassNode cn = app.findClassNode(owner);
		if(!checkNullClass(cn, owner)) {
			return null;
		}
		
		for(MethodNode mn : cn.getMethods()) {
			if(mn.getName().equals(name) && mn.getDesc().equals(desc)) {
				return mn;
			}
		}
		
		return resolveStaticCall(cn.node.superName, name, desc);
	}

	@Override
	public MethodNode resolveVirtualInitCall(String owner, String desc) {
		ClassNode cn = app.findClassNode(owner);
		if(!checkNullClass(cn, owner)) {
			return null;
		}
		
		MethodNode mn = resolve(cn, "<init>", desc, true);
		if(mn == null) {
			return null;
		} else if(!mn.owner.getName().equals(owner)) {
			throw new UnsupportedOperationException(mn.toString());
		} else {
			return mn;
		}
	}

	@Override
	public Set<MethodNode> resolveVirtualCalls(String owner, String name, String desc, boolean strict) {
		/* find concrete receivers and resolve */
		ClassNode cn = app.findClassNode(owner);
		if(!checkNullClass(cn, owner)) {
			return Collections.emptySet();
		}
		
		Set<MethodNode> result = new HashSet<>();
		
		for(ClassNode receiver : app.getClassTree().getAllChildren(cn)) {
			if(!Modifier.isAbstract(receiver.node.access)) {
				// use strict mode = false for incomplete analysis
				MethodNode target = resolve(receiver, name, desc, true);
				
				if(target == null|| Modifier.isAbstract(target.node.access)) {
					throw new IllegalStateException(String.format("Could not find vtarget for %s.%s%s", owner, name, desc));
				}
				
				result.add(target);
			}
		}
		
		return result;
	}
	
	// FIXME: these are taken directly from the old resolver

	@Override
	public FieldNode findStaticField(String owner, String name, String desc) {
		Set<FieldNode> set = new HashSet<>();
		
		ClassNode cn = app.findClassNode(owner);
		
		/* we do this because static fields can be in
		 * interfaces. */
		if(cn != null) {
			Set<ClassNode> lvl = new HashSet<>();
			lvl.add(cn);
			for(;;) {
				if(lvl.size() == 0) {
					break;
				}
				
				Set<FieldNode> lvlSites = new HashSet<>();
				
				for(ClassNode c : lvl) {
					for(FieldNode f : c.getFields()) {
						if(Modifier.isStatic(f.node.access) && f.getName().equals(name) && f.getDesc().equals(desc)) {
							lvlSites.add(f);
						}
					}
				}
				
				if(lvlSites.size() > 1) {
					LOGGER.info(String.format("(warn) resolved %s.%s %s to %s", owner, name, desc, lvlSites));
				}
				
				if(lvlSites.size() > 0) {
					set.addAll(lvlSites);
					break;
				}
				
				Set<ClassNode> newLvl = new HashSet<>();
				for(ClassNode c : lvl) {
					ClassNode sup = app.findClassNode(c.node.superName);
					if(sup != null) {
						newLvl.add(sup);
					}
					
					for(String iface : c.node.interfaces) {
						ClassNode ifaceN = app.findClassNode(iface);
						
						if(ifaceN != null) {
							newLvl.add(ifaceN);
						}
					}
				}
				
				lvl.clear();
				lvl = newLvl;
			}
		}
		
		if(set.size() > 1) {
			throw new UnsupportedOperationException(String.format("multi dispatch?: %s.%s %s results:%s", owner, name, desc, set));
		} else if(set.size() == 1) {
			return set.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public FieldNode findVirtualField(String owner, String name, String desc) {
		ClassNode cn = app.findClassNode(owner);

		if (cn != null) {
			do {
				for (FieldNode f : cn.getFields()) {
					if (!Modifier.isStatic(f.node.access) && f.getName().equals(name) && f.getDesc().equals(desc)) {
						return f;
					}
				}

				cn = app.findClassNode(cn.node.superName);
			} while (cn != null);
		}

		return null;
	}

	@Override
	public Set<MethodNode> getHierarchyMethodChain(ClassNode cn, String name, String desc, boolean exact) {
		return hierarchyMethodsHelper.getHierarchyMethodChain(cn, name, desc, exact);
	}

	// debug methods
	
	/* returns true iff the node is not null. if the node is null
	 * and allowMissingClasses is turned off we spaz out. */
	private static boolean checkNullClass(ClassNode n, String expected) {
		if(n == null) {
			if(allowMissingClasses) {
				return false;
			} else {
				throw new IllegalStateException(String.format("Could not find %s in hierarchy", expected));
			}
		}
		
		return true;
	}
	
	protected void print(Map<Selector, MethodNode> m) {
		for(Entry<Selector, MethodNode> e : m.entrySet()) {
			LOGGER.debug("   " + e.getValue());
		}
	}
	
	protected void printMap(Map<Selector, Set<MethodNode>> m) {
		for (Entry<Selector, Set<MethodNode>> e : m.entrySet()) {
			LOGGER.debug("     " + e.getKey());
			for (MethodNode n : e.getValue()) {
				LOGGER.debug("       " + n);
			}
		}
	}
	
	private <N> void assertIntersection(Set<N> s1, Set<N> s2, Set<N> expected) {
		if(debugLevel >= 1) {
			Set<N> tmp = new HashSet<>();
			tmp.addAll(s1);
			tmp.retainAll(s2);
			
			if(!tmp.equals(expected)) {
				throw new IllegalStateException(String.format("Intersection of %s and %s = %s, expected %s", s1, s2, tmp, expected));
			}
		}
	}
	
	public static final class Selector {

		public final String name;
		public final String desc;

		public Selector(String name, String desc) {
			this.name = name;
			this.desc = desc;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Selector) {
				Selector other = (Selector) o;
				return name.equals(other.name) && desc.equals(other.desc);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return 19 * name.hashCode() * desc.hashCode();
		}

		@Override
		public String toString() {
			return name + desc;
		}
	}
}
