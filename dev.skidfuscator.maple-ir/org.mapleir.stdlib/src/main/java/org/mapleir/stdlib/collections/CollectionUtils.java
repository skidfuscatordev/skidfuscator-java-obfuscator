package org.mapleir.stdlib.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mapleir.stdlib.collections.map.ValueCreator;

public class CollectionUtils {

	public static <T, K> Map<T, K> copyOf(Map<T, K> src) {
		Map<T, K> dst = new HashMap<>();
		copy(src, dst);
		return dst;
	}
	
	public static <T, K> void copy(Map<T, K> src, Map<T, K> dst) {
		for(Entry<T, K> e : src.entrySet()) {
			dst.put(e.getKey(), e.getValue());
		}
	}
	
	public static <T> List<T> collate(Iterator<T> it){
		List<T> list = new ArrayList<>();
		while(it.hasNext()) {
			list.add(it.next());
		}
		return list;
	}
	
	@SafeVarargs
	public static <E, C extends Collection<E>> C asCollection(ValueCreator<C> vc, E... elements) {
		C col = vc.create();
		if(elements != null) {
			for(E e : elements) {
				if(e != null) {
					col.add(e);
				}
			}
		}
		return col;
	}
}