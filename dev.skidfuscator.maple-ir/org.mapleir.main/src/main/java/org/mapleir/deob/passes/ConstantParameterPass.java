package org.mapleir.deob.passes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mapleir.app.service.InvocationResolver;
import org.mapleir.context.AnalysisContext;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassResult;
import org.mapleir.deob.interproc.IPAnalysis;
import org.mapleir.deob.interproc.IPAnalysisVisitor;
import org.mapleir.deob.passes.rename.MethodRenamerPass;
import org.mapleir.deob.util.RenamingUtil;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.mapleir.ir.utils.CFGUtils;
import org.objectweb.asm.Type;
import org.mapleir.asm.MethodNode;

// TODO: Convert to use TaintableSet
public class ConstantParameterPass implements IPass, Opcode {

	@Override
	public PassResult accept(PassContext pcxt) {
		AnalysisContext cxt = pcxt.getAnalysis();
		Map<MethodNode, Set<MethodNode>> chainMap = new HashMap<>();
		for(MethodNode mn : cxt.getIRCache().getActiveMethods()) {
			makeUpChain(cxt, mn, chainMap);
		}
		
		InvocationResolver resolver = cxt.getInvocationResolver();
		
		Map<MethodNode, List<Set<Object>>> rawConstantParameters = new HashMap<>();
		Map<MethodNode, boolean[]> chainedNonConstant = new HashMap<>();
		Map<MethodNode, boolean[]> specificNonConstant = new HashMap<>();
		
		IPAnalysisVisitor vis = new IPAnalysisVisitor() {
			@Override
			public void postVisitMethod(IPAnalysis analysis, MethodNode m) {
				int pCount = Type.getArgumentTypes(m.getDesc()).length;
				
				/* init map entries */
				if(!chainedNonConstant.containsKey(m)) {
					for(MethodNode assoc : chainMap.get(m)) {
						boolean[] arr = new boolean[pCount];
						chainedNonConstant.put(assoc, arr);
					}
					
					for(MethodNode assoc : chainMap.get(m)) {
						boolean[] arr = new boolean[pCount];
						specificNonConstant.put(assoc, arr);
					}
				}
				
				if(Modifier.isStatic(m.node.access)) {
					if(!rawConstantParameters.containsKey(m)) {
						List<Set<Object>> l = new ArrayList<>(pCount);
						rawConstantParameters.put(m, l);
						
						for(int i=0; i < pCount; i++) {
							l.add(new HashSet<>());
						}
					}
				} else {
					// TODO: cache
					for(MethodNode site : resolver.resolveVirtualCalls(m, true)) {
						if(!rawConstantParameters.containsKey(site)) {
							List<Set<Object>> l = new ArrayList<>(pCount);
							rawConstantParameters.put(site, l);
							
							for(int i=0; i < pCount; i++) {
								l.add(new HashSet<>());
							}
						}
					}
				}
			}
			
			@Override
			public void postProcessedInvocation(IPAnalysis analysis, MethodNode caller, MethodNode callee, Invocation call) {
				Expr[] params = call.getParameterExprs();
				
				for(int i=0; i < params.length; i++) {
					Expr e = params[i];
					
					if(e.getOpcode() == Opcode.CONST_LOAD) {
						if(Modifier.isStatic(callee.node.access)) {
							rawConstantParameters.get(callee).get(i).add(((ConstantExpr) e).getConstant());
						} else {
							/* only chain callsites *can* have this input */
							for(MethodNode site : resolver.resolveVirtualCalls(callee, true)) {
								rawConstantParameters.get(site).get(i).add(((ConstantExpr) e).getConstant());
							}
						}
					} else {
						// FIXME:
						/* whole branch tainted */
						for(MethodNode associated : chainMap.get(callee)) {
							chainedNonConstant.get(associated)[i] = true;
						}
						
						/* callsites tainted */
						if(Modifier.isStatic(callee.node.access)) {
							specificNonConstant.get(callee)[i] = true;
						} else {
							/* only chain callsites *can* have this input */
							for(MethodNode site : resolver.resolveVirtualCalls(callee, true)) {
								specificNonConstant.get(site)[i] = true;
							}
						}
					}
				}
			}
		};
		
		IPAnalysis constAnalysis = IPAnalysis.create(cxt, vis);
		
//		ApplicationClassSource app = cxt.getApplication();
//		ClassTree structures = app.getStructures();
		
		/* remove all calls to library methods since we can't
		 * handle them. */
		/*Iterator<Entry<MethodNode, List<Set<Object>>>> it = rawConstantParameters.entrySet().iterator();
		while(it.hasNext()) {
			Entry<MethodNode, List<Set<Object>>> en = it.next();
			
			MethodNode m = en.getKey();

			if(app.isLibraryClass(m.owner.getName())) {
				it.remove();
				continue;
			}
			
			// TODO: MUST BE CONVERTED TO ACCOUNT FOR DIRECT SUPERS, NOT ALL
			superFor: for(ClassNode cn : structures.getAllParents(m.owner)) {
				if(app.isLibraryClass(cn.getName())) {
					for(MethodNode m1 : cn.methods) {
						if(resolver.areMethodsCongruent(m1, m, Modifier.isStatic(m.node.access))) {
							it.remove();
							break superFor;
						}
					}
				}
			}
		}*/
		
		/* aggregate constant parameters indices with their chained
		 * methods such that the map contains only constant parameter
		 * indices that we can actually remove while keeping a valid chain.
		 * 
		 * We do this as we can have methods from different branches that
		 * are cousin-related but have different constant parameter values.
		 * In these cases we can still inline the constants (different constants)
		 * and change the descriptions, keeping the chain. */
		
		Map<MethodNode, boolean[]> filteredConstantParameters = new HashMap<>();
		
		for(Entry<MethodNode, List<Set<Object>>> en : rawConstantParameters.entrySet()) {
			MethodNode m = en.getKey();

			List<Set<Object>> objParams = en.getValue();
			boolean[] tainted = chainedNonConstant.get(m);
			
			if(filteredConstantParameters.containsKey(m)) {
				/* note: if this method is contained in the
				 * map all of it's cousin-reachable methods
				 * must also be and furthermore the dead map
				 * for the entire chain is the same array. 
				 * 
				 * we need to now merge the current dead map
				 * with the one specifically for this method.*/
				
				boolean[] thisDeadMap = makeDeadMap(objParams, tainted);
				boolean[] prevDeadMap = filteredConstantParameters.get(m);
				
				if(thisDeadMap.length != prevDeadMap.length) {
					throw new IllegalStateException(String.format("m: %s, chain:%s, %d:%d", m, chainMap.get(m), thisDeadMap.length, prevDeadMap.length));
				}
				
				/* each dead map contains true values for an
				 * index if that index is a constant parameter. */
				for(int i=0; i < prevDeadMap.length; i++) {
					prevDeadMap[i] &= thisDeadMap[i];
				}
			} else {
				boolean[] deadParams = makeDeadMap(objParams, tainted);
				
				for(MethodNode chm : chainMap.get(m)) {
					filteredConstantParameters.put(chm, deadParams);
				}
			}
			
			ControlFlowGraph cfg = cxt.getIRCache().getFor(m);
			
			// boolean b = false;
			
			boolean[] specificTaint = specificNonConstant.get(m);
			
			for(int i=0; i < objParams.size(); i++) {
				Set<Object> set = objParams.get(i);
				
				/* since these are callsite specific
				 * constant parameters, we can inline
				 * them even if we can't eliminate the
				 * parameter for the whole chain later.
				 * 
				 * doing this here also means that when
				 * we rebuild descriptors later, if the
				 * parameter */
				
				if(!specificTaint[i] && set.size() == 1) {
					inlineConstant(cfg, constAnalysis.getLocalIndex(m, i), set.iterator().next());
				}
			}
		}
		
		Map<MethodNode, String> remap = new HashMap<>();
		Set<MethodNode> toRemove = new HashSet<>();
		
		Set<Set<MethodNode>> mustRename = new HashSet<>();
		
		for(Entry<MethodNode, boolean[]> en : filteredConstantParameters.entrySet()) {
			MethodNode m = en.getKey();
			
			if(!remap.containsKey(m) && !toRemove.contains(m)) {
				boolean[] deadMap = en.getValue();
				
				boolean notSame = false;
				for(boolean b : deadMap) {
					notSame |= b;
				}
				
				if(!notSame) {
					/* eliminate all branches (same congruence class) */
					for(MethodNode n : chainMap.get(m)) {
						toRemove.add(n);
					}
					continue;
				}
				
				Type[] params = Type.getArgumentTypes(m.getDesc());
				Type ret = Type.getReturnType(m.getDesc());
				String desc = buildDesc(params, ret, deadMap);
				
				Set<MethodNode> conflicts = new HashSet<>();
				
				for(MethodNode chm : chainMap.get(m)) {
					remap.put(chm, desc);
					
					if(Modifier.isStatic(m.node.access)) {
						MethodNode mm = resolver.resolveStaticCall(chm.owner.getName(), chm.getName(), desc);
						if(mm != null) {
							conflicts.add(mm);
						}
					} else {
						if(chm.getName().equals("<init>")) {
							conflicts.addAll(resolver.resolveVirtualCalls(chm.owner.getName(), "<init>", desc, false));
						} else {
							conflicts.addAll(resolver.getHierarchyMethodChain(m.owner, m.getName(), desc, true));
						}
					}
				}
				
				if(conflicts.size() > 0) {
					Set<MethodNode> chain = chainMap.get(m);
					
					/* rename the smallest conflict set */
//					if(chain.size() < conflicts.size()) {
//						
//					} else {
//						mustRename.add(conflicts);
//					}
					mustRename.add(chain);
				}
			}
		}
		
		remap.keySet().removeAll(toRemove);
		
		int k = RenamingUtil.numeric("aaaaa");
		Map<MethodNode, String> methodNameRemap = new HashMap<>();
		for(Set<MethodNode> set : mustRename) {
			// MethodNode first = set.iterator().next();
			// String newName = "rename_" + first.getName();
			String newName = RenamingUtil.createName(k++);
			System.out.printf(" renaming %s to %s%n", set, newName);
			System.out.println("   recom " + computeChain(cxt, set.iterator().next()));
			Set<MethodNode> s2 = new HashSet<>();
			for(MethodNode m : set) {
				s2.addAll(chainMap.get(m));
			}
			
			if(!s2.equals(set)) {
				System.err.println(set);
				System.err.println(s2);
				throw new IllegalStateException();
			}
			
			for(MethodNode m : set) {
				methodNameRemap.put(m, newName);
			}
		}
		
		if(mustRename.size() > 0) {
			MethodRenamerPass.rename(cxt, methodNameRemap, false);
		}
		
		Set<MethodNode> visitedMethods = new HashSet<>();
		Set<Expr> visitedExprs = new HashSet<>();
		
		int killedTotal = 0;
		for(;;) {
			int killedBeforePass = killedTotal;
			
			for(Entry<MethodNode, String> en : remap.entrySet()) {
				MethodNode key = en.getKey();
				String newDesc = en.getValue();
				
				if(!visitedMethods.contains(key)) {
					Set<MethodNode> chain = chainMap.get(key);
					
					/*for(MethodNode n : chain) {
						if(visitedMethods.contains(n)) {
							throw new IllegalStateException(String.format("Invalid transistivityr: %s in %s but not %s", n, chain, key));
						}
					}*/
					
					boolean[] dead = filteredConstantParameters.get(key);
					
					for(MethodNode n : chain) {
						n.node.desc = newDesc;
						
						/* boolean[] dead = filteredConstantParameters.get(n);
						boolean[] deadM = filteredConstantParameters.get(key);
						
						if(!Arrays.equals(dead, deadM)) {
							throw new IllegalStateException(String.format("neq: %s vs %s for %s and %s", Arrays.toString(dead), Arrays.toString(deadM), n, key));
						} */
						
						demoteDeadParamters(constAnalysis, cxt.getIRCache().getFor(n), n, dead);
						
						for(Invocation call : constAnalysis.getCallsTo(n)) {
							/* since the callgrapher finds all
							 * the methods in a hierarchy and considers
							 * it as a single invocation, a certain
							 * invocation may be considered multiple times. */
							if(visitedExprs.contains(call)) {
								continue;
							}
							
							/* the invocationexpr method desc is changed implicitly
							 * when the new expression is created in patchCall() */
							visitedExprs.add(call);
							patchCall(newDesc, call, dead);
							
							killedTotal += chain.size();
						}
					}
					
					visitedMethods.addAll(chain);
				}
			}
			
			if(killedBeforePass == killedTotal) {
				break;
			}
		}
		
		System.out.printf("  removed %d constant parameters.%n", killedTotal);
		return PassResult.with(pcxt, this).finished(killedTotal).make();
	}
	
	private void inlineConstant(ControlFlowGraph cfg, int argLocalIndex, Object o) {
		/* we don't actually demote the synthetic copy
		 * here as we would also need to change the
		 * method desc and we can't do that until
		 * later so we defer it. */
		LocalsPool pool = cfg.getLocals();

		/* create the spill variable but not the
		 * actual definition yet. */
		Local argLocal = pool.get(argLocalIndex, 0, false);
		VersionedLocal spill = pool.makeLatestVersion(argLocal);

		AbstractCopyStmt synthParamCopy = pool.defs.get(argLocal);
		ConstantExpr rhsVal = new ConstantExpr(o, synthParamCopy.getType() == Type.BOOLEAN_TYPE ? Type.BYTE_TYPE : synthParamCopy.getType());
		
		/* we have to maintain local references in
		 * phis as opposed to direct constant refs,
		 * so we go through every use of the argLocal
		 * and either replace it with the constant or
		 * a reference to the spill local if it is in
		 * a phi. */

		Set<VarExpr> spillUses = new HashSet<>();
		boolean requireSpill = false;
		
		Iterator<VarExpr> it = pool.uses.get(argLocal).iterator();
		while(it.hasNext()) {
			VarExpr v = it.next();
			if(v.getParent() == null) {
				/* the use is in a phi, we can't
				 * remove the def. 
				 * 
				 * we also replace the old var
				 * with the new spill one so we
				 * have to add this as a use of
				 * the new spill local. */
				spillUses.add(v);
				v.setLocal(spill);
				
				requireSpill = true;
			} else {
				CodeUnit par = v.getParent();
				par.writeAt(rhsVal.copy(), par.indexOf(v));
			}
			
			/* this use is no longer associated
			 * with the old argLocal. */
			it.remove();
		}
		
		if(pool.uses.get(argLocal).size() != 0) {
			throw new IllegalStateException(String.format("l:%s, uses:%s", argLocal, pool.uses.get(argLocal)));
		}
		
		if(requireSpill) {
			/* generate the copy for the spill (v = const) */
			CopyVarStmt spillCopy = new CopyVarStmt(new VarExpr(spill, synthParamCopy.getVariable().getType()), rhsVal);
			synthParamCopy.getBlock().add(spillCopy);
			
			/* initialise data entries for the new spill
			 * variable. */
			pool.defs.put(spill, spillCopy);
			pool.uses.put(spill, spillUses);
		}
	}
	
	private void demoteDeadParamters(IPAnalysis constAnalysis, ControlFlowGraph cfg, MethodNode n, boolean[] dead) {
		LocalsPool pool = cfg.getLocals();
		BasicBlock entry = cfg.getEntries().iterator().next();
		
		for(int i=0; i < dead.length; i++) {
			if(dead[i]) {
				int localIndex = constAnalysis.getLocalIndex(n, i);
				VersionedLocal local = pool.get(localIndex, 0, false);
				
				AbstractCopyStmt copy = pool.defs.get(local);
				
				if(copy.getBlock() != entry) {
					System.err.printf("entry:%n%s%n", CFGUtils.printBlock(entry));
					System.err.printf("block:%n%s%n", CFGUtils.printBlock(copy.getBlock()));
					throw new IllegalStateException(String.format("See debug trace (entry vs block) in %s", n));
				}
				
				copy.getBlock().remove(copy);
				
				if(pool.uses.get(local).size() != 0) {
					throw new IllegalStateException(String.format("m: %s, l:%s, uses:%s", n, local, pool.uses.get(local)));
				}

				pool.defs.remove(local);
				pool.uses.remove(local);
			}
		}
	}

	private void patchCall(String newDesc, Expr call, boolean[] dead) {
		if(call.getOpcode() == Opcode.INIT_OBJ) {
			InitialisedObjectExpr init = (InitialisedObjectExpr) call;

			CodeUnit parent = init.getParent();
			Expr[] newArgs = buildArgs(init.getArgumentExprs(), false, dead);
			InitialisedObjectExpr init2 = new InitialisedObjectExpr(init.getOwner(), newDesc, newArgs);

			parent.writeAt(init2, parent.indexOf(init));
		} else if(call.getOpcode() == Opcode.INVOKE) {
			InvocationExpr invoke = (InvocationExpr) call;
			if (invoke.isDynamic())
				throw new UnsupportedOperationException(call.toString());

			CodeUnit parent = invoke.getParent();
			
			Expr[] newArgs = buildArgs(invoke.getArgumentExprs(), invoke.getCallType() != InvocationExpr.CallType.STATIC, dead);
			InvocationExpr invoke2 = invoke.copy();
			
			parent.writeAt(invoke2, parent.indexOf(invoke));
		} else {
			throw new UnsupportedOperationException(call.toString());
		}
	}
	
	private static Expr[] buildArgs(Expr[] oldArgs, boolean virtual, boolean[] dead) {
		int off = virtual ? 1 : 0;
		
		if(dead.length != (oldArgs.length - off)) {
			throw new IllegalStateException();
		}
		
		List<Expr> newArgs = new ArrayList<>(oldArgs.length);
		for(int i=dead.length-1; i >= 0; i--) {
			Expr e = oldArgs[i + off];
			if(!dead[i]) {
				newArgs.add(0, e);
			}
			e.unlink();
		}
		
		if(virtual) {
			Expr e = oldArgs[0];
			newArgs.add(0, e);
			e.unlink();
		}
		
		return newArgs.toArray(new Expr[0]);
	}
	
	private static boolean[] makeDeadMap(List<Set<Object>> objParams, boolean[] tainted) {
		boolean[] removable = new boolean[objParams.size()];
		
		for(int i=0; i < objParams.size(); i++) {
			Set<Object> s = objParams.get(i);
			
			removable[i] = s.size() <= 1 && !tainted[i];
		}
		
		return removable;
	}
	
	private static String buildDesc(Type[] preParams, Type ret, boolean[] dead) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for(int i=0; i < preParams.length; i++) {
			if(!dead[i]) {
				Type t = preParams[i];
				sb.append(t.toString());
			}
		}
		sb.append(")").append(ret.toString());
		return sb.toString();
	}
	
	private Set<MethodNode> computeChain(AnalysisContext cxt, MethodNode m) {
		Set<MethodNode> chain = new HashSet<>();
		chain.add(m);
		
		if(!Modifier.isStatic(m.node.access)) {
			if(!m.getName().equals("<init>")) {
				chain.addAll(cxt.getInvocationResolver().getHierarchyMethodChain(m.owner, m.getName(), m.getDesc(), true));
			}
		}
		
		return chain;
	}
		
	private void makeUpChain(AnalysisContext cxt, MethodNode m, Map<MethodNode, Set<MethodNode>> chainMap) {
		if(chainMap.containsKey(m)) {
			/*Set<MethodNode> chain = chainMap.get(m);
			Set<MethodNode> comp = computeChain(cxt, m);
			if(!chain.equals(comp)) {
				throw new IllegalStateException(m + "\n chain: " + chain +"\n comp: " + comp);
			}*/
		} else {
			Set<MethodNode> chain = computeChain(cxt, m);
			for(MethodNode chm : chain) {
				chainMap.put(chm, chain);
			}
		}
	}
}
