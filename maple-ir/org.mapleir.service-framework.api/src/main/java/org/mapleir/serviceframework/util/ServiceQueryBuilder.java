package org.mapleir.serviceframework.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mapleir.serviceframework.api.IServiceQuery;
import org.mapleir.serviceframework.api.IServiceReference;

public class ServiceQueryBuilder<T> {
	
	public enum Mode {
		MATCH_ANY, MATCH_ALL
	}
	
	private final List<IServiceQuery<T>> queries;
	private Mode mode;
	
	public ServiceQueryBuilder() {
		queries = new ArrayList<>();
		mode = null;
	}
	
	public ServiceQueryBuilder<T> setMode(Mode mode) {
		this.mode = mode;
		return this;
	}
	
	public Mode getMode() {
		return this.mode;
	}
	
	@SafeVarargs
	public static <T> ServiceQueryBuilder<T> from(IServiceQuery<T>... queries) {
		return null;
	}
	
	private final Collection<IServiceQuery<T>> _getInputQueries() {
		return Collections.unmodifiableCollection(queries);
	}
	
	public IServiceQuery<T> build() {
		switch (mode) {
		case MATCH_ALL:
			return new MatchAllServiceQueryIteratingQuery<T>(_getInputQueries());
		case MATCH_ANY:
			return new MatchAnyServiceQueryIteratingQuery<T>(_getInputQueries());
		default:
			throw new UnsupportedOperationException("No mode");
		}
	}
	
	private static abstract class ServiceQueryIteratingQuery<T> implements IServiceQuery<T> {
		final Collection<IServiceQuery<T>> queries;
		final ServiceQueryBuilder.Mode mode;
		
		ServiceQueryIteratingQuery(Collection<IServiceQuery<T>> queries, ServiceQueryBuilder.Mode mode) {
			this.queries = queries;
			this.mode = mode;
		}
		
		@Override
		public abstract boolean accept(IServiceReference<T> ref);
		
		public String toString() {
			return String.format("%s on %d queries", mode.name(), queries.size());
		}
	}
	
	private static class MatchAnyServiceQueryIteratingQuery<T> extends ServiceQueryIteratingQuery<T> {
		MatchAnyServiceQueryIteratingQuery(Collection<IServiceQuery<T>> queries) {
			super(queries, ServiceQueryBuilder.Mode.MATCH_ANY);
		}

		@Override
		public boolean accept(IServiceReference<T> ref) {
			for(IServiceQuery<T> q : queries) {
				if(q.accept(ref)) {
					return true;
				}
			}
			
			return false;
		}
	}
	
	private static class MatchAllServiceQueryIteratingQuery<T> extends ServiceQueryIteratingQuery<T> {
		MatchAllServiceQueryIteratingQuery(Collection<IServiceQuery<T>> queries) {
			super(queries, ServiceQueryBuilder.Mode.MATCH_ALL);
		}

		@Override
		public boolean accept(IServiceReference<T> ref) {
			for(IServiceQuery<T> q : queries) {
				if(!q.accept(ref)) {
					return false;
				}
			}
			
			return true;
		}
	}

	public ServiceQueryBuilder<T> add(IServiceQuery<T> q) {
		queries.add(q);
		return this;
	}

	public ServiceQueryBuilder<T> add(int index, IServiceQuery<T> element) {
		queries.add(index, element);
		return this;
	}

	public ServiceQueryBuilder<T> addAll(Collection<? extends IServiceQuery<T>> c) {
		queries.addAll(c);
		return this;
	}

	public ServiceQueryBuilder<T> addAll(int index, Collection<? extends IServiceQuery<T>> c) {
		queries.addAll(index, c);
		return this;
	}

	public ServiceQueryBuilder<T> clear() {
		queries.clear();
		return this;
	}

	public boolean contains(IServiceQuery<T> q) {
		return queries.contains(q);
	}

	public boolean containsAll(Collection<?> c) {
		return queries.containsAll(c);
	}

	public IServiceQuery<T> get(int index) {
		return queries.get(index);
	}

	public int indexOf(IServiceQuery<T> q) {
		return queries.indexOf(q);
	}

	public boolean isEmpty() {
		return queries.isEmpty();
	}

	public Iterator<IServiceQuery<T>> iterator() {
		return queries.iterator();
	}

	public int lastIndexOf(IServiceQuery<T> q) {
		return queries.lastIndexOf(q);
	}

	public ServiceQueryBuilder<T> remove(IServiceQuery<T> q) {
		queries.remove(q);
		return this;
	}

	public ServiceQueryBuilder<T> remove(int index) {
		queries.remove(index);
		return this;
	}

	public ServiceQueryBuilder<T> removeAll(Collection<?> c) {
		queries.removeAll(c);
		return this;
	}

	public ServiceQueryBuilder<T> retainAll(Collection<?> c) {
		queries.retainAll(c);
		return this;
	}

	public ServiceQueryBuilder<T> set(int index, IServiceQuery<T> element) {
		queries.set(index, element);
		return this;
	}

	public int size() {
		return queries.size();
	}
}