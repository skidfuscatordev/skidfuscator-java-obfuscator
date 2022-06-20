package org.mapleir.stdlib.collections.graph.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.mapleir.stdlib.collections.graph.FastGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

import static junit.framework.Assert.*;

public class CollectionUtil {

	public static <E> Collection<E> asList(E... es) { 
		Collection<E> col = new ArrayList<>();
		for(E e : es) {
			col.add(e);
		}
		return col;
	}
	
	public static <N extends FastGraphVertex, E extends FastGraphEdge<N>> Set<E> getEdges(FastGraph<N, E> g) {
		Set<E> res = new HashSet<>();
		for (N o : g.vertices()) {
			res.addAll(g.getEdges(o));
		}
		return res;
	}
	
	public static <E extends FastGraphEdge<?>> void assertContainsEdges(Collection<E> actual, Collection<E> expecting) {
		assertEquals(expecting.size(), actual.size());

		Iterator<E> expectIt = expecting.iterator();
		outer: while (expectIt.hasNext()) {
			E ex = expectIt.next();

			for (E ac : actual) {
				if (ac.src() == ex.src() && ac.dst() == ex.dst()) {
					continue outer;
				}
			}

			fail(String.format("%s doesn't contain %s", actual, ex));
		}
	}
}
