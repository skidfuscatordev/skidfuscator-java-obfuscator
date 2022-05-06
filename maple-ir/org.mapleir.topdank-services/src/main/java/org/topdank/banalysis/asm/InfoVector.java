package org.topdank.banalysis.asm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.topdank.banalysis.filter.Filter;

public abstract class InfoVector<T> {
	
	protected Map<Integer, T> numericalMap;
	protected Map<T, Integer> retrievalMap;
	
	public InfoVector(List<T> ts) {
		this(ts, true, new Filter<T>() {
			@Override
			public boolean accept(T t) {
				return true;
			}
		});
	}
	
	public InfoVector(List<T> ts, boolean definiteCount, Filter<T> filter) {
		numericalMap = new HashMap<Integer, T>();
		retrievalMap = new HashMap<T, Integer>();
		if (definiteCount) {
			buildAbsoluteMap(ts, filter);
		} else {
			buildRelativeMap(ts, filter);
		}
	}
	
	public void buildAbsoluteMap(List<T> ts, Filter<T> filter) {
		for(int i = 0; i < ts.size(); i++) {
			T t = ts.get(i);
			if (filter.accept(t)) {
				numericalMap.put(i, ts.get(i));
				retrievalMap.put(ts.get(i), i);
			}
		}
	}
	
	public void buildRelativeMap(List<T> ts, Filter<T> filter) {
		int count = 0;
		for(T t : ts) {
			if (filter.accept(t)) {
				numericalMap.put(count, t);
				retrievalMap.put(t, count);
				count++;
			}
		}
	}
	
	public T getAt(int index) {
		return numericalMap.get(index);
	}
}