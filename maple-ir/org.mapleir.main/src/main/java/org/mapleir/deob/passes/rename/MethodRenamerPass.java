package org.mapleir.deob.passes.rename;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mapleir.app.client.SimpleApplicationContext;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.InvocationResolver;
import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.deob.util.RenamingHeuristic;
import org.mapleir.deob.util.RenamingUtil;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public class MethodRenamerPass implements IPass {

	private final RenamingHeuristic heuristic;
	
	public MethodRenamerPass(RenamingHeuristic heuristic) {
		this.heuristic = heuristic;
	}
	
	@Override
	public PassResult accept(PassContext pcxt) {
		AnalysisContext cxt = pcxt.getAnalysis();
		ApplicationClassSource source = cxt.getApplication();
		InvocationResolver resolver = cxt.getInvocationResolver();
		
		Map<MethodNode, String> remapped = new HashMap<>();

		int totalMethods = 0;
		
		for(ClassNode cn : source.iterate()) {
			totalMethods += cn.getMethods().size();
		}
		
		int i = RenamingUtil.computeMinimum(totalMethods);
		
		// Map<MethodNode, Set<MethodNode>> debugMap = new HashMap<>();
		
		for(ClassNode cn : source.iterate()) {
			for(MethodNode m : cn.getMethods()) {
				if (!heuristic.shouldRename(m.getName(), m.node.access)) {
				// 	System.out.println("Heuristic bypass meth " + m.name);
					continue;
				}
				if(remapped.containsKey(m)) {
					continue;
				}
				
				if(Modifier.isStatic(m.node.access)) {
					if(!m.getName().equals("<clinit>") && !SimpleApplicationContext.isMainMethod(m)) {
						String newName = RenamingUtil.createName(i++);
						remapped.put(m, newName);
					}
				} else {
					if(!m.getName().equals("<init>")) {
						// Set<ClassNode> classes = source.getStructures().dfsTree(m.owner, true, true, true);
						// Set<MethodNode> methods = getVirtualMethods(cxt, classes, m.name, m.desc);
						Set<MethodNode> methods = resolver.getHierarchyMethodChain(m.owner, m.getName(), m.node.desc, true);
						if(canRename(cxt, methods)) {
							String newName = RenamingUtil.createName(i++);
							
							for(MethodNode o : methods) {
								// Set<MethodNode> s2 = InvocationResolver.getHierarchyMethodChain(cxt, o.owner, o.name, m.desc, true);
								
								/*if(!methods.equals(s2)) {
									System.err.printf("m: %s%n", m);
									System.err.printf("o: %s%n", o);
									System.err.println("this ms::");
									for(MethodNode s : methods) {
										System.err.printf("   %s%n", s);
									}
									System.err.println("o ms::");
									for(MethodNode s : s2) {
										System.err.printf("   %s%n", s);
									}
									throw new IllegalStateException();
								}*/
								
								/*if(remapped.containsKey(o)) {
									System.err.printf("m: %s%n", m);
									System.err.printf("o: %s%n", o);
									System.err.println("this ms::");
									for(MethodNode s : methods) {
										System.err.printf("   %s%n", s);
									}
									System.err.println("o ms::");
									for(MethodNode s : InvocationResolver.getHierarchyMethodChain(cxt, o.owner, o.name, m.desc, true)) {
										System.err.printf("   %s%n", s);
									}
									System.err.println(" o debugset::");
									for(MethodNode s : debugMap.get(o)) {
										System.err.printf("   %s%n", s);
									}
									System.err.printf("on: %s%n", remapped.get(o));
									System.err.printf("nn: %s%n", newName);
									throw new IllegalStateException();
								}*/
								remapped.put(o, newName);
							}
							
							/*for(MethodNode hm : methods) {
								debugMap.put(hm, methods);
							}*/
						} else {
							System.out.println("  can't rename: " + methods);
						}
					}
				}
			}
		}
		
		rename(cxt, remapped, true);
		System.out.printf("  Remapped %d/%d methods.%n", remapped.size(), totalMethods);

		return PassResult.with(pcxt, this).finished().make();
	}
	
	public static void rename(AnalysisContext cxt, Map<MethodNode, String> remapped, boolean warn) {
		ApplicationClassSource source = cxt.getApplication();
		InvocationResolver resolver = cxt.getInvocationResolver();
		
		for(ClassNode cn : source.iterate()) {
			{
				if(cn.node.outerMethod != null) {
//					ClassNode owner = tree.getClass(cn.node.outerClass);
					System.out.println("Outer: " + cn.node.outerClass + "." + cn.node.outerMethod + " " + cn.node.outerMethodDesc);
					cn.node.outerClass = null;
					cn.node.outerMethod = null;
					cn.node.outerMethodDesc = null;
					//					System.out.println(owner.name);
//					do {
//						for(MethodNode m : owner.methods) {
//							System.out.println(m);
//							if(m.name.equals(cn.outerMethod) && m.desc.equals(cn.outerMethodDesc)) {
//								System.out.println("m: " + m);
//							}
//						}
//						owner = tree.getClass(owner.superName);
//						System.out.println(cn.superName);
//						System.out.println(owner);
//					} while(owner != null);
				}
			}
			
			Set<Expr> visited = new HashSet<>();
			
			for(MethodNode m : cn.getMethods()) {
				ControlFlowGraph cfg = cxt.getIRCache().getFor(m);
				
				for(BasicBlock b : cfg.vertices()) {
					for(Stmt stmt : b) {
						for(Expr e : stmt.enumerateOnlyChildren()) {
							
							if(e.getOpcode() == Opcode.INVOKE) {
								InvocationExpr invoke = (InvocationExpr) e;
								if (invoke.isDynamic())
									throw new UnsupportedOperationException();
								
								if(visited.contains(invoke)) {
									throw new RuntimeException(invoke.toString());
								}
								visited.add(invoke);
								
								if(invoke.getOwner().startsWith("[")) {
									System.err.println("  ignore array object invoke: " + invoke + ", owner: " + invoke.getOwner());
									continue;
								}
								
								if(invoke.isStatic()) {
									MethodNode site = resolver.resolveStaticCall(invoke.getOwner(), invoke.getName(), invoke.getDesc());
									
									if(site != null) {
										if(remapped.containsKey(site)) {
											invoke.setName(remapped.get(site));
										} else {
											if(warn && mustMark(source, invoke.getOwner())) {
												System.err.println("  invalid site(s): " + invoke);
											}
										}
									} else {
										if(mustMark(source, invoke.getOwner())) {
											System.err.printf("  can't resolve(s) %s ; %s.%s %s%n", invoke, invoke.getOwner(), invoke.getName(), invoke.getDesc());

											/*if(m.toString().equals("hey.IIiIiiIiII()Lime;")) {
												System.out.println("MethodRenamerPass.accept() " + newName);
												throw new UnsupportedOperationException();
											}*/
											
											if(invoke.getOwner().equals("hey")) {
												for(MethodNode mm : cxt.getApplication().findClassNode(invoke.getOwner()).getMethods()) {
													System.out.println(mm);
												}
												throw new UnsupportedOperationException();
											}
										}
									}
								} else {
//									 Set<MethodNode> sites = resolver.resolveVirtualCalls(invoke.getOwner(), invoke.getName(), invoke.getDesc());
									// Set<ClassNode> classes = source.getStructures().dfsTree(cn, true, true, true);
									// Set<MethodNode> sites = getVirtualMethods(cxt, classes, invoke.getName(), invoke.getDesc());
									Set<MethodNode> sites = resolver.getHierarchyMethodChain(source.findClassNode(invoke.getOwner()), invoke.getName(), invoke.getDesc(), true);
									if(sites.size() > 0) {
										/* all of the sites must be linked by the same name,
										 * so we can use any to find the new name. */
										
										boolean anyContains = false;
										boolean allContains = true;
										for(MethodNode s : sites) {
											anyContains |= remapped.containsKey(s);
											allContains &= remapped.containsKey(s);
										}
										
										if(anyContains && !allContains) {
											System.err.println("mismatch: ");
											// System.err.println(classes);
											System.err.println(sites);
											throw new RuntimeException();
										}
										
										MethodNode site = sites.iterator().next();
										if(remapped.containsKey(site)) {
											invoke.setName(remapped.get(site));
										} else {
											if(warn && !site.getName().equals("<init>") && canRename(cxt, sites)) {
												System.err.println("  invalid site(v): " + invoke + ", " + sites);
											}
										}
									} else {
										if(mustMark(source, invoke.getOwner())) {
											System.err.println("  can't resolve(v) " + invoke + ", owner: " + invoke.getOwner() + " desc " + invoke.getDesc());
											// System.err.println("  classes: " + classes);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		/* Rename the methods after as we need to resolve
		 * them using the old names during the invocation 
		 * analysis above. */
		for(Entry<MethodNode, String> e : remapped.entrySet()) {
			// System.out.printf("%s -> %s%n", e.getKey(), e.getValue());
			e.getKey().node.name = e.getValue();
		}
	}
	
	private static boolean mustMark(ApplicationClassSource tree, String owner) {
		ClassNode cn = tree.findClassNode(owner);
		return cn == null || !tree.isLibraryClass(owner);
	}
	
	private static boolean canRename(AnalysisContext cxt, Set<MethodNode> methods) {
		for(MethodNode m : methods) {
			if(cxt.getApplication().isLibraryClass(m.getOwner())) {
				/* inherited from runtime class */
				return false;
			}
		}
		return true;
	}
	
	/*public static MethodNode findClassMethod(ClassNode cn, String name, String desc) {
		MethodNode findM = null;
		
		for(MethodNode m : cn.methods) {
			if(!Modifier.isStatic(m.access)) {
				if(m.name.equals(name) && m.desc.equals(desc)) {
					
					if(findM != null) {
						throw new IllegalStateException(String.format("%s contains %s and %s", cn.name, findM, m));
					}
					
					findM = m;
				}
			}
		}
		
		return findM;
	}*/
}
