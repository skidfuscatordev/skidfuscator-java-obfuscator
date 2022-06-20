package org.mapleir.dot4j.model;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;
import org.mapleir.dot4j.attr.SimpleAttributed;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;

public class Node implements Attributed<Node>, Source<Node>, Target, Connected {

	protected ComplexLabel name;
	protected List<Edge> edges;
	protected Attributed<Node> attributes;
	
	Node() {
		this(null, new ArrayList<>(), Attrs.attrs());
	}
	
	public Node(ComplexLabel name, List<Edge> edges, Attrs attributes) {
		this.edges = edges;
		this.attributes = new SimpleAttributed<>(this, attributes);
		setName(name);
	}
	
	public ComplexLabel getName() {
		return name;
	}
	
	public Node copy() {
		return new Node(name, new ArrayList<>(edges), attributes.applyTo(Attrs.attrs()));
	}
	
	public Node setName(ComplexLabel name) {
		this.name = name;
		if(name != null) {
			if(name.isExternal()) {
				this.name = ComplexLabel.of("");
				attributes.with(name);
			} else if(name.isHtml()) {
				attributes.with(name);
			}
		}
		return this;
	}
	
	public Node setName(String name) {
		return setName(ComplexLabel.of(name));
	}
	
	public Node merge(Node n) {
		edges.addAll(n.edges);
		attributes.with(n.attributes);
		return this;
	}
	
	public PortNode withRecord(String record) {
		return new PortNode().setNode(this).setRecord(record);
	}
	
	public PortNode withPosition(Compass position) {
		return new PortNode().setNode(this).setCompass(position);
	}

	@Override
	public Attrs applyTo(MapAttrs mapAttrs) {
		return attributes.applyTo(mapAttrs);
	}

	@Override
	public Node with(Attrs attrs) {
		attributes.with(attrs);
		return this;
	}

	@Override
	public Object get(String key) {
		return attributes.get(key);
	}
	
	@Override
	public Collection<Edge> getEdges() {
		return edges;
	}

	@Override
	public Edge edgeToHere() {
		return Edge.to(this);
	}

	@Override
	public Node addEdge(Target target) {
		Edge edge = target.edgeToHere();
		edges.add(Edge.between(getSource(edge), edge.getTarget()).with(edge.getAttrs()));
		return this;
	}
	
	public Node addEdge(String node) {
		return addEdge(Factory.node(name));
	}
	
	public Node addEdges(Target... targets) {
		for(Target target : targets) {
			addEdge(target);
		}
		return this;
	}
	
	public Node addEdges(String... names) {
		for(String name : names) {
			addEdge(name);
		}
		return this;
	}
	
	private Source<?> getSource(Edge edge) {
		if(edge.getSource() instanceof PortNode) {
			PortNode n = (PortNode) edge.getSource();
			return new PortNode().setNode(this).setRecord(n.getRecord()).setCompass(n.getPosition());
		}
		return new PortNode().setNode(this);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Node node = (Node) o;
		if (name != null ? !name.equals(node.name) : node.name != null) {
			return false;
		}
		if (edges != null ? !edges.equals(node.edges) : node.edges != null) {
			return false;
		}
		return !(attributes != null ? !attributes.equals(node.attributes) : node.attributes != null);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (edges != null ? edges.hashCode() : 0);
		result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return name + attributes.toString() + "->"
				+ edges.stream().map(l -> l.getTarget().toString()).collect(joining(","));
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return attributes.iterator();
	}
}
