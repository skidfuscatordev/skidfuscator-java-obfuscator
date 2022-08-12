package org.mapleir.ir.cfg;

import org.mapleir.ir.code.ExpressionPool;
import org.mapleir.ir.code.ExpressionStack;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.TypeStack;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.list.NotifiedList;

import java.util.*;

import static org.mapleir.stdlib.util.StringHelper.createBlockName;

public class BasicBlock implements FastGraphVertex, Collection<Stmt> {

	/**
	 * Specifies that this block should not be merged in later passes.
	 */
	public static final int FLAG_NO_MERGE = 0x1;

	/**
	 * Two blocks A, B, must have A.id == B.id IFF A == B
	 * Very important!
	 */
	private int id;
	public final ControlFlowGraph cfg;
	private final NotifiedList<Stmt> statements;
	private TypeStack stack;
	private ExpressionPool pool;
	private int flags = 0;

	// for debugging purposes. the number of times the label was changed
	private int relabelCount = 0;

	public BasicBlock(ControlFlowGraph cfg) {
		this.cfg = cfg;
		this.id = cfg.makeBlockId();
		statements = new NotifiedList<>(
				(s) -> s.setBlock(this),
				(s) -> {
					if (s.getBlock() == this)
						s.setBlock(null);
				}
		);
	}
	
	public boolean isFlagSet(int flag) {
		return (flags & flag) == flag;
	}
	
	public void setFlag(int flag, boolean b) {
		if(b) {
			flags |= flag;
		} else {
			flags ^= flag;
		}
	}
	
	public void setFlags(int flags) {
		this.flags = flags;
	}

	public int getFlags() {
		return flags;
	}
	
	public ControlFlowGraph getGraph() {
		return cfg;
	}

	public void transfer(BasicBlock dst) {
		Iterator<Stmt> it = statements.iterator();
		while(it.hasNext()) {
			Stmt s = it.next();
			it.remove();
			dst.add(s);
			assert (s.getBlock() == dst);
		}
	}

	/**
	 * Transfers statements up to index `to`, exclusively, to block `dst`.
	 */
	public void transferUpto(BasicBlock dst, int to) {
		// FIXME: faster
		for(int i=to - 1; i >= 0; i--) {
			Stmt s = remove(0);
			dst.add(s);
			assert (s.getBlock() == dst);
		}
	}

	@Override
	public String getDisplayName() {
		return createBlockName(id);
	}

	/**
	 * If you call me you better know what you are doing.
	 * If you use me in any collections, they must be entirely rebuilt from scratch
	 * ESPECIALLY indexed or hash-based ones.
	 * This includes collections of edges too.
	 * @param i newId
	 */
	public void setId(int i) {
		relabelCount++;
		id = i;
	}
	
	@Override
	public int getNumericId() {
		return id;
	}

	@Override
	public String toString() {
		return String.format("Block #%s", createBlockName(id)/* (%s), label != null ? label.hashCode() : "dummy"*/);
	}

	// This implementation of equals doesn't really do anything, it's just for sanity-checking purposes.
	// NOTE: we can't change equals or hashCode because the id can change from ControlFlowGraph#relabel.
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		BasicBlock bb = (BasicBlock) o;

		if (id == bb.id) {
			assert (relabelCount == bb.relabelCount);
			assert (this == bb);
		}
		return id == bb.id;
	}
	
	public void checkConsistency() {
		for (Stmt stmt : statements)
			if (stmt.getBlock() != this)
				throw new IllegalStateException("Orphaned child " + stmt);
	}

	// List functions
	@Override
	public boolean add(Stmt stmt) {
		return statements.add(stmt);
	}

	public void add(int index, Stmt stmt) {
		statements.add(index, stmt);
	}

	@Override
	public boolean remove(Object o) {
		return statements.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return statements.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Stmt> c) {
		return statements.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends Stmt> c) {
		return statements.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return statements.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return statements.retainAll(c);
	}

	public Stmt remove(int index) {
		return statements.remove(index);
	}

	@Override
	public boolean contains(Object o) {
		return statements.contains(o);
	}

	@Override
	public boolean isEmpty() {
		return statements.isEmpty();
	}

	public int indexOf(Stmt o) {
		return statements.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return statements.lastIndexOf(o);
	}

	public Stmt get(int index) {
		return statements.get(index);
	}

	public Stmt set(int index, Stmt stmt) {
		return statements.set(index, stmt);
	}

	@Override
	public int size() {
		return statements.size();
	}

	@Override
	public void clear() {
		statements.clear();
	}

	public TypeStack getStack() {
		return stack;
	}

	public void setStack(TypeStack stack) {
		this.stack = stack;
	}

	public ExpressionPool getPool() {
		return pool;
	}

	public void setPool(ExpressionPool pool) {
		this.pool = pool;
	}

	@Override
	public Iterator<Stmt> iterator() {
		return statements.iterator();
	}

	public ListIterator<Stmt> listIterator() {
		return statements.listIterator();
	}

	public ListIterator<Stmt> listIterator(int index) {
		return statements.listIterator(index);
	}

	@Override
	public Object[] toArray() {
		return statements.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return statements.toArray(a);
	}
	// End list functions
}
