package org.mapleir.stdlib.collections.map;

import java.util.HashMap;
import java.util.Map;

public abstract class CachedKeyedValueCreator<K, V> implements KeyedValueCreator<K, V> {
	private final Map<K, V> map = makeMapImpl();
	
	protected Map<K, V> makeMapImpl() {
		return new HashMap<>();
	}
	
	public Map<K, V> getMap() {
		return map;
	}

	protected abstract V create0(K k);
	
	@Override
	public V create(K k) {
		if(map.containsKey(k)) {
			return map.get(k);
		} else {
			V v = create0(k);
			map.put(k, v);
			return v;
		}
	}
	
	public static class DelegatingCachedKeyedValueCreator<K, V> extends CachedKeyedValueCreator<K, V> {
		
		private final KeyedValueCreator<K, V> child;

		@SuppressWarnings("unchecked")
		public DelegatingCachedKeyedValueCreator(ValueCreator<V> child) {
			/* implying the retard doesn't pass in something that
			 * depends on the key XD */
			this((KeyedValueCreator<K, V>)child);
		}
		
		public DelegatingCachedKeyedValueCreator(KeyedValueCreator<K, V> child) {
			this.child = child;
			
			if(child == null) {
				throw new NullPointerException();
			}
		}


		@Override
		protected V create0(K k) {
			return child.create(k);
		}
	}
}