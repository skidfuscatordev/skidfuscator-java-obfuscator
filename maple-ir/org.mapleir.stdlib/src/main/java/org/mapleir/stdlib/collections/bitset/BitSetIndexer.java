package org.mapleir.stdlib.collections.bitset;

public interface BitSetIndexer<N> {
	int getIndex(N n);
	
	N get(int index);
	
	boolean isIndexed(N o);
}
