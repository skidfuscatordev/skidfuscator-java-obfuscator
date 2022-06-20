package org.mapleir.dot4j.model;

import java.util.Iterator;
import java.util.Map.Entry;

import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;
import org.mapleir.dot4j.attr.SimpleAttributed;

public class Edge implements Attributed<Edge>, Target {

	private final Source<?> source;
	private final Target target;
	private final Attributed<Edge> attributes;
	
	public static Edge to(Node node) {
		return to(node.withRecord(null));
	}
	
	public static Edge to(Target to) {
		return makeEdge(null, to);
	}
	
	public static Edge between(Source<?> source, Target target) {
		return makeEdge(source, target);
	}
	
	private static Edge makeEdge(Source<?> source, Target target) {
		return Context.createEdge(source, target);
	}
	
	Edge(Source<?> source, Target target, Attrs attributes) {
		this.source = source;
		this.target = target;
		this.attributes = new SimpleAttributed<>(this, attributes);
	}
	
	public Source<?> getSource() {
		return source;
	}
	
	public Target getTarget() {
		return target;
	}
	
	public Attributed<Edge> getAttrs() {
		return attributes;
	}
	
	@Override
	public Attrs applyTo(MapAttrs mapAttrs) {
		return attributes.applyTo(mapAttrs);
	}

	@Override
	public Edge edgeToHere() {
		return this;
	}

	@Override
	public Edge with(Attrs attrs) {
		attributes.with(attrs);
		return this;
//		return new Edge(source, target, attrs.apply(attributes.apply(Attrs.attrs())));
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o == null || getClass() != o.getClass()) {
			return false;
		}
		Edge e = (Edge) o;
		return attributes.equals(e.attributes);
	}
	
	@Override
	public int hashCode() {
		return attributes.hashCode();
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		throw new UnsupportedOperationException();
	}
}
