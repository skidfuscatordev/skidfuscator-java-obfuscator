package org.mapleir.flowgraph;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.util.StringHelper;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * THIS IS NOT IMMUTABLE SO DON'T OVERRIDE HASHCODE/EQUALS!!!!
 * @param <N>
 */
public class ExceptionRange<N extends FastGraphVertex> {

	private final List<N> nodes;
	private final Set<Type> types;
	private N handler;
	
	public ExceptionRange() {
		nodes = new ArrayList<>();
		types = new HashSet<>();
	}

	public void setHandler(N b) {
	
		handler = b;
	}
	
	public N getHandler() {
		return handler;
	}

	public boolean containsVertex(N b) {
		return nodes.contains(b);
	}
	
	public void addVertex(N b) {
		nodes.add(b);
	}
	
	public void addVertexAfter(N b, N s) {
		nodes.add(nodes.indexOf(b), s);
	}
	
	public void addVertexBefore(N b, N s) {
		nodes.add(nodes.indexOf(b), s);
	}
	
	public void addVertices(Collection<N> col) {
		nodes.addAll(col);
	}
	
	public void addVertices(N pos, Collection<N> col) {
		nodes.addAll(nodes.indexOf(pos), col);
	}
	
	public void removeVertex(N b) {
		nodes.remove(b);
	}
	
	public Set<Type> getTypes() {
		return new HashSet<>(types);
	}
	
	public void addType(Type b) {
		types.add(b);
	}
	
	public void removeType(Type b) {
		types.remove(b);
	}
	
	public void setTypes(Set<Type> types) {
		this.types.clear();
		this.types.addAll(types);
	}

	public void clearNodes() {
		nodes.clear();
	}

	public void reset() {
		nodes.clear();
		types.clear();
		handler = null;
	}

	public boolean isCircular() {
		return nodes.contains(handler);
	}

	@Override
	public String toString() {
		return String.format("handler=%s, types=%s, range=%s", handler, types, rangetoString(nodes));
	}

	// FIXME: can't rely on numeric for random graphs
	public static <N extends FastGraphVertex> String rangetoString(List<N> set) {
		if(set.size() == 0) {
			return set.toString();
		}
		
		// FIXME: verify this works as intended after getId removed
		int last = set.get(0).getNumericId() - 1;
		for(int i=0; i < set.size(); i++) {
			int num = set.get(i).getNumericId();
			if((last + 1) == num) {
				last++;
				continue;
			} else {
				return set.toString();
			}
		}
		
		return String.format("[#%s...#%s]", set.get(0).getDisplayName(), StringHelper.createBlockName(last));
	}
	
	public List<N> getNodes() {
		return Collections.unmodifiableList(nodes);
	}
}
