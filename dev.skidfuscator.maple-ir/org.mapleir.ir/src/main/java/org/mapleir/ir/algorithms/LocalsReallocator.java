package org.mapleir.ir.algorithms;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.objectweb.asm.Type;

import java.util.*;

public class LocalsReallocator {
    public static int realloc(ControlFlowGraph cfg) {
        LocalsPool locals = cfg.getLocals();
        NullPermeableHashMap<Local, Set<Type>> types = new NullPermeableHashMap<>(HashSet::new);
        int min = 0;
        Set<Local> safe = new HashSet<>();
        for(BasicBlock b : cfg.vertices()) {
            for(Stmt stmt : b) {
                if(stmt.getOpcode() == Opcode.LOCAL_STORE) {
                    CopyVarStmt cp = (CopyVarStmt) stmt;
                    VarExpr var = cp.getVariable();
                    Local local = var.getLocal();
                    if(!cp.isSynthetic()) {
                        types.getNonNull(local).add(var.getType());
                    } else {
                        safe.add(local);
                    }
                    types.getNonNull(local).add(var.getType());
                }

                for(Expr s : stmt.enumerateOnlyChildren()) {
                    if(s.getOpcode() == Opcode.LOCAL_LOAD) {
                        VarExpr var = (VarExpr) s;
                        Local local = var.getLocal();
                        types.getNonNull(local).add(var.getType());
                    }
                }
            }
        }

        Map<Local, Type> stypes = new HashMap<>();

        for(Map.Entry<Local, Set<Type>> e : types.entrySet()) {
            Set<Type> set = e.getValue();
            Set<Type> refined = new HashSet<>();
            if(set.size() > 1) {
                for(Type t : set) {
                    refined.add(TypeUtils.asSimpleType(t));
                }
                if(refined.size() != 1) {
                    boolean valid = false;

                    if(refined.size() == 2) {
                        // TODO: proper check
                        Iterator<Type> it = refined.iterator();
                        if(it.next().getSize() == it.next().getSize()) {
                            Type t = refined.iterator().next();
                            refined.clear();
                            refined.add(t);
                            valid = true;
                        }
                    }

                    if(!valid) {
                        for(Map.Entry<Local, Set<Type>> e1 : types.entrySet()) {
                            System.err.println(e1.getKey() + "  ==  " + e1.getValue());
                        }
                        // String.format("illegal typesets for %s, set=%s, refined=%s", args)
                        throw new RuntimeException("illegal typesets for " + e.getKey());
                    }
                }
                Local l = e.getKey();
                stypes.put(l, refined.iterator().next());

//				if(!safe.contains(l)) {
//					stypes.put(l, refined.iterator().next());
//				}
            } else {
                Local l = e.getKey();
                stypes.put(l, set.iterator().next());
//				if(!safe.contains(l)) {
//				}
            }
        }

//		for(Entry<Local, Type> e : stypes.entrySet()) {
//			System.out.println(e.getKey() + "  ==  " + e.getValue());
//		}

        // lvars then svars, ordered of course,
        List<Local> wl = new ArrayList<>(stypes.keySet());
//		System.out.println("safe: " + safe);
        wl.sort(new Comparator<Local>() {
            @Override
            public int compare(Local o1, Local o2) {
                boolean s1 = safe.contains(o1);
                boolean s2 = safe.contains(o2);

                if (s1 && !s2) {
                    return -1;
                } else if (!s1 && s2) {
                    return 1;
                } else {
                    VersionedLocal vo1 = o1 instanceof VersionedLocal ? (VersionedLocal) o1 : cfg.getLocals().get(o1.getIndex(), 0);
                    VersionedLocal vo2 = o2 instanceof VersionedLocal ? (VersionedLocal) o2 : cfg.getLocals().get(o2.getIndex(), 0);
                    return vo1.compareTo(vo2);
                }
            }
        });
//		System.out.println("wl: " + wl);

        Map<Local, Local> remap = new HashMap<>();
        int idx = min;
        for(Local l : wl) {
            Type type = stypes.get(l);
            Local newL = locals.get(idx, false);
            newL.setType(type);
            if(l != newL) {
                remap.put(l, newL);
            }
            idx += type.getSize();
        }
        remap(cfg, remap);

        return idx;
    }

    public static void remap(ControlFlowGraph cfg, Map<? extends Local, ? extends Local> remap) {
        for(BasicBlock b : cfg.vertices()) {
            for(Stmt stmt : b) {
                if(stmt.getOpcode() == Opcode.LOCAL_STORE) {
                    VarExpr v = ((CopyVarStmt) stmt).getVariable();
                    Local l = v.getLocal();
                    if(remap.containsKey(l)) {
                        Local l2 = remap.get(l);
                        v.setLocal(l2);
                    }
                }

                for(Expr s : stmt.enumerateOnlyChildren()) {
                    if(s.getOpcode() == Opcode.LOCAL_LOAD) {
                        VarExpr v = (VarExpr) s;
                        Local l = v.getLocal();
                        if(remap.containsKey(l)) {
                            v.setLocal(remap.get(l));
                        }
                    }
                }
            }
        }
        LocalsPool pool = cfg.getLocals();
        for (Map.Entry<? extends Local, ? extends Local> e : remap.entrySet()) {
            if (e.getKey() instanceof VersionedLocal) {
                VersionedLocal from = (VersionedLocal) e.getKey();
                if (e.getValue() instanceof VersionedLocal) {
                    VersionedLocal to = (VersionedLocal) e.getKey();
                    if (pool.defs.containsKey(from)) {
                        pool.defs.put(to, pool.defs.get(from));
                    }
                    if (pool.uses.containsKey(from)) {
                        pool.uses.put(to, pool.uses.get(from));
                    }
                } else {
                    pool.defs.remove(from);
                    pool.uses.remove(from);
                }
            }
        }
    }
}
