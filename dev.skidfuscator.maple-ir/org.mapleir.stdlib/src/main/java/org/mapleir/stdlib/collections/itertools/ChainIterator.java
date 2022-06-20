package org.mapleir.stdlib.collections.itertools;

import java.util.Collection;
import java.util.Iterator;

public abstract class ChainIterator<T> implements Iterator<T> {
	
	protected Iterator<T> current;
	
	private boolean findNext() {
		Iterator<T> nextIt = nextIterator();
		if(nextIt == null) {
			return false;
		} else {
			/* If this next iterator has no
			 * elements in it, the next call to
			 * hasNext will get a new iterator,
			 * checking it until we find a
			 * valid one. */
			current = nextIt;
			return hasNext(nextIt);
		}
	}
	
	private boolean hasNext(Iterator<T> it) {
		if(it == null) {
			return findNext();
		} else {
			if(it.hasNext()) {
				return true;
			} else {
				return findNext();
			}
		}
	}
	@Override
	public boolean hasNext() {
		return hasNext(current);
	}

	@Override
	public T next() {
		return current.next();
	}
	
	public abstract Iterator<T> nextIterator();
	
	public static class CollectionChainIterator<T> extends ChainIterator<T> {

		private final Iterator<? extends Collection<T>> it;
		
		public CollectionChainIterator(Collection<? extends Collection<T>> collections) {
			this.it = collections.iterator();
		}

		@Override
		public Iterator<T> nextIterator() {
			if(it.hasNext()) {
				return it.next().iterator();
			} else {
				return null;
			}
		}
	}
}