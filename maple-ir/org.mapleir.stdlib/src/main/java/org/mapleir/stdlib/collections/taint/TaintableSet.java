package org.mapleir.stdlib.collections.taint;

import org.mapleir.stdlib.collections.itertools.ProductIterator;
import org.mapleir.stdlib.util.Pair;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TaintableSet<T> implements Set<T>, ITaintable {
	private final Set<T> backingSet;
	private boolean tainted;
	
	public TaintableSet(boolean dirty) {
		this();
		tainted = dirty;
	}
	
	public TaintableSet() {
		backingSet = new HashSet<>();
		tainted = false;
	}
	
	public TaintableSet(TaintableSet<T> other) {
		backingSet = new HashSet<>(other);
		tainted = other.tainted;
	}

	public Iterator<Pair<T, T>> product(TaintableSet<T> other) {
		return ProductIterator.getIterator(this, other);
	}
	
	/**
	 * Mark as tainted.
	 */
	public void taint() {
		tainted = true;
	}
	
	@Override
	public boolean isTainted() {
		return tainted;
	}
	
	@Override
	public boolean union(ITaintable t) {
		if (t instanceof Collection) {
			backingSet.addAll((Collection) t);
		} else if (t != null) {
			backingSet.add((T) t);
		}
		return tainted |= t.isTainted();
	}
	
	@Override
	public int size() {
		return backingSet.size();
	}

	@Override
	public boolean isEmpty() {
		return backingSet.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return backingSet.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return backingSet.iterator();
	}

	@Override
	public Object[] toArray() {
		return backingSet.toArray();
	}

	@Override
	public <T2> T2[] toArray(T2[] a) {
		return backingSet.toArray(a);
	}

	@Override
	public boolean add(T e) {
		return backingSet.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return backingSet.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c instanceof TaintableSet) {
			TaintableSet ts = (TaintableSet) c;
			if (ts.isTainted())
				return false;
			return backingSet.containsAll(ts.backingSet);
		}
		return backingSet.containsAll(c);
	}
	
	// Do not call this on other TaintableSets; call union instead.
	@Deprecated @Override
	public boolean addAll(Collection<? extends T> c) {
		return backingSet.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return backingSet.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return backingSet.removeAll(c);
	}

	@Override
	public void clear() {
		backingSet.clear();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		
		TaintableSet<?> that = (TaintableSet<?>) o;
		
		if (tainted != that.tainted)
			return false;
		return backingSet.equals(that.backingSet);
	}
	
	@Override
	public int hashCode() {
		int result = backingSet.hashCode();
		result = 31 * result + (tainted ? 1 : 0);
		return result;
	}
	
	@Override
	public String toString() {
		return backingSet.toString() + " (tainted=" + tainted + ")";
	}
}