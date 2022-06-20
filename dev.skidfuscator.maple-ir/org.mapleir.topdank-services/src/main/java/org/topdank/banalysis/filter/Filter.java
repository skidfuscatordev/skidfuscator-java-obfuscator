package org.topdank.banalysis.filter;

public abstract interface Filter<T> {
	
	public static final Filter<Object> ACCEPT_ALL = new Filter<Object>() {
		@Override
		public boolean accept(Object t) {
			return true;
		}
	};
	
	@SuppressWarnings("unchecked")
	public static <T> Filter<T> acceptAll(){
		return (Filter<T>) ACCEPT_ALL;
	}
	
	public abstract boolean accept(T t);
}