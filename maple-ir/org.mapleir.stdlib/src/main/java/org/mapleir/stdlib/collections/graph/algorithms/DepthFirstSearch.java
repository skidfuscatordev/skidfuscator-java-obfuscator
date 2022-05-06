package org.mapleir.stdlib.collections.graph.algorithms;

import java.util.List;

public interface DepthFirstSearch<N> {

	List<N> getPreOrder();
	
	List<N> getPostOrder();

	List<N> getTopoOrder();
}
