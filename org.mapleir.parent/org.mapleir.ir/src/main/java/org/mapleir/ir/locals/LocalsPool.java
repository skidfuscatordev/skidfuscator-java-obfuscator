package org.mapleir.ir.locals;

import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.impl.BasicLocal;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.mapleir.stdlib.collections.bitset.BitSetIndexer;
import org.mapleir.stdlib.collections.bitset.GenericBitSet;
import org.mapleir.stdlib.collections.bitset.IncrementalBitSetIndexer;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.mapleir.stdlib.collections.map.ValueCreator;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

public abstract class LocalsPool implements ValueCreator<GenericBitSet<Local>> {

	private final Map<String, Local> cache;
	private final Map<BasicLocal, VersionedLocal> latest;
	private final BitSetIndexer<Local> indexer;
	private int maxLocals, maxStack;

	public final Map<VersionedLocal, AbstractCopyStmt> defs;
	public final NullPermeableHashMap<VersionedLocal, Set<VarExpr>> uses;

	public LocalsPool() {
		cache = new HashMap<>();
		latest = new HashMap<>();
		indexer = new IncrementalBitSetIndexer<>();
		maxLocals = maxStack = 0;

		defs = new HashMap<>();
		uses = new NullPermeableHashMap<>(HashSet::new);
	}

	public abstract boolean isReservedRegister(Local l);
	
	public abstract boolean isImplicitRegister(Local l);
	
	public Set<Local> getAll(Predicate<Local> p)  {
		Set<Local> set = new HashSet<>();
		for(Local l : cache.values()) {
			if(p.test(l)) {
				set.add(l);
			}
		}
		return set;
	}
	
	// factory
	public GenericBitSet<Local> createBitSet() {
		return new GenericBitSet<>(indexer);
	}

	@Override
	public GenericBitSet<Local> create() {
		return createBitSet();
	}
	public Map<String, Local> getCache() {
		return cache;
	}
	// end factory

	public BasicLocal asSimpleLocal(Local l) {
		return get(l.getIndex(), l.isStack());
	}
	
	public VersionedLocal makeLatestVersion(Local l) {
		VersionedLocal vl = getLatestVersion(l);
		return get(vl.getIndex(), vl.getSubscript() + 1, vl.isStack());
	}
	
	public VersionedLocal getLatestVersion(Local l) {
		l = asSimpleLocal(l);
		if(!latest.containsKey(l)) {
			return get(l.getIndex(), 0, l.isStack());
		} else {
			return latest.get(l);
		}
	}

	public List<Local> getOrderedList() {
		List<Local> list = new ArrayList<>();
		list.addAll(cache.values());
		Collections.sort(list);
		return list;
	}

	private void updateMaxs(int index, boolean isStack) {
		if (isStack) // this is why we need ternary as lvalue.
			maxStack = Math.max(maxStack, index);
		else
			maxLocals = Math.max(maxLocals, index);
	}
	
	public VersionedLocal get(int index, int subscript) {
		return get(index, subscript, false);
	}
	
	public VersionedLocal get(int index, int subscript, boolean isStack) {
		updateMaxs(index, isStack);
		String key = key(index, subscript, isStack);
		if(cache.containsKey(key)) {
			return (VersionedLocal) cache.get(key);
		} else {
			VersionedLocal v = new VersionedLocal(index, subscript, isStack);
			cache.put(key, v);
			
			BasicLocal bl = get(index, isStack);
			if(latest.containsKey(bl)) {
				VersionedLocal old = latest.get(bl);
				if(subscript > old.getSubscript()) {
					latest.put(bl, v);
				} else if(subscript == old.getSubscript()) {
					throw new IllegalStateException("Created " + v + " with " + old + ", " + bl);
				}
			} else {
				latest.put(bl, v);
			}
			
			return v;
		}
	}
	
	public BasicLocal get(int index) {
		return get(index, false);
	}
	
	public BasicLocal get(int index, boolean isStack) {
		updateMaxs(index, isStack);
		String key = key(index, isStack);
		if(cache.containsKey(key)) {
			return (BasicLocal) cache.get(key);
		} else {
			BasicLocal v = new BasicLocal(index, isStack);
			cache.put(key, v);
			return v;
		}
	}

	public BasicLocal newLocal(int i, boolean isStack) {
		while(true) {
			String key = key(i, isStack);
			if(!cache.containsKey(key)) {
				return get(i, isStack);
			}
			i++;
		}
	}
	
	public BasicLocal getNextFreeLocal(boolean isStack) {
		return newLocal(0, isStack);
	}

	public int getMaxLocals() {
		return maxLocals;
	}

	public int getMaxStack() {
		return maxStack;
	}
	
	/* public Local newLocal(boolean isStack) {
		int index = cache.size();
		while(true) {
			String key = key(index, isStack);
			if(!cache.containsKey(key)) {
				return get(index, isStack);
			}
		}
	} */

	public static String key(int index, boolean stack) {
		return (stack ? "s" : "l") + "var" + index;
	}
	
	public static String key(int index, int subscript, boolean stack) {
		return (stack ? "s" : "l") + "var" + index + "_" + subscript;
	}
}
