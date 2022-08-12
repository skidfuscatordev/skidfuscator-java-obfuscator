package org.mapleir.stdlib.collections.itertools;

import org.mapleir.stdlib.util.Pair;

import java.util.Iterator;

public abstract class ProductIterator<T> implements Iterator<Pair<T, T>> {
	
	public static <T> ProductIterator<T> getIterator(Iterable<T> a, Iterable<T> b) {
		Iterator<T> iteratorA = a.iterator();
		if(iteratorA.hasNext()) {
			Iterator<T> iteratorB = b.iterator();
			if(iteratorB.hasNext()) {
				return new BasicProductIterator<>(a, b);
			}
		}
		
		return new EmptyProductIterator<>();
	}
	
	static class BasicProductIterator<T> extends ProductIterator<T> {
		private Iterator<T> iteratorA;
		private Iterator<T> iteratorB;
		
		private T curA;
		private Iterable<T> b;
		
		public BasicProductIterator(Iterable<T> a, Iterable<T> b) {
			this.iteratorA = a.iterator();
			this.iteratorB = b.iterator();
			
			this.curA = null;
			this.b = b;
		}
		
		// FIXME: doesnt work if any of the iterables in the constructor
		// are empty
		@Override
		public boolean hasNext() {
			if (!iteratorB.hasNext()) {
				if (!iteratorA.hasNext()) {
					return false; // at the corner; end
				}
				// at end of row; start over B and move to next A
				curA = iteratorA.next();
				iteratorB = b.iterator();
			}
			return true;
		}
		
		@Override
		public Pair<T, T> next() {
			if (curA == null)
				curA = iteratorA.next();
			return new Pair<>(curA, iteratorB.next());
		}
	}
	
	static class EmptyProductIterator<T> extends ProductIterator<T> {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Pair<T, T> next() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public abstract Pair<T, T> next();
}
