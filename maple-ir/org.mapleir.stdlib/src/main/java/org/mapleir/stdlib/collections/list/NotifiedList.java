package org.mapleir.stdlib.collections.list;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * An ArrayList but with callbacks for when an element is removed or added.
 * This is really a pretty big kludge since ArrayList's internals aren't fixed,
 * and subclassing ArrayList is generally a bad idea anyways.
 *
 * Yes, this *does* acount for the Iterator's remove, add, etc.
 *
 * @param <E> element type
 */
public class NotifiedList<E> extends ArrayList<E> implements Serializable {
	private final Consumer<E> onAdded;
	private final Consumer<E> onRemoved;

	public NotifiedList(Consumer<E> onAdded, Consumer<E> onRemoved) {
		super();
		this.onAdded = onAdded;
		this.onRemoved = onRemoved;
	}

	// Hooked functions
	@Override
	public boolean add(E elem) {
		if (elem != null)
			onAdded.accept(elem);
		return super.add(elem);
	}

	@Override
	public void add(int index, E elem) {
		if (elem != null)
			onAdded.accept(elem);
		super.add(index, elem);
	}

	@Override
	public boolean remove(Object o) {
		boolean ret = super.remove(o);
		if (ret && o != null)
			onRemoved.accept((E) o);
		return ret;
	}

	@Override
	public E remove(int index) {
		E oldElem = super.remove(index);
		if (oldElem != null)
			onRemoved.accept(oldElem);
		return oldElem;
	}

	@Override
	public E set(int index, E elem) {
		if (elem != null)
			onAdded.accept(elem);
		E oldElem = super.set(index, elem);
		if (oldElem != null)
			onRemoved.accept(oldElem);
		return oldElem;
	}

	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		for (int i = fromIndex; i < toIndex; i++) {
			E elem = get(i);
			if (elem != null)
				onRemoved.accept(elem);
		}
		super.removeRange(fromIndex, toIndex);
	}

	// Overridden collective updates
	@Override
	public boolean addAll(Collection<? extends E> c) {
		for (E elem : c)
			add(elem);
		return c.size() != 0;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		for (E elem : c)
			add(index++, elem);
		return c.size() != 0;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean ret = false;
		for (Object o : c)
			ret = remove(o) || ret; // keep in mind that must be after the || due to short-circuiting
		return ret;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean ret = false;
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			E elem = it.next();
			if (!c.contains(elem)) {
				it.remove();
				ret = true;
			}
		}
		return ret;
	}

	@Override
	public void clear() {
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			E s = it.next();
			it.remove();
		}
	}

	// Blocked functions
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		// the sublist implementation directly modifies elementData. No bueno.
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}

	private void readObject(java.io.ObjectInputStream s) {
		throw new UnsupportedOperationException();
	}

	private void writeObject(java.io.ObjectOutputStream s) {
		throw new UnsupportedOperationException();
	}
}
