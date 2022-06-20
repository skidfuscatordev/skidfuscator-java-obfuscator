package org.mapleir.dot4j.model;

import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;

public final class Factory {

	private Factory() {
	}
	
	public static DotGraph graph(String name) {
		return graph().setName(name);
	}
	
	public static DotGraph graph() {
		return Context.createGraph();
	}
	
	public static Node node(String name) {
		return node(ComplexLabel.of(name));
	}
	
	public static Node node(ComplexLabel label) {
		return Context.createNode(label);
	}
	
	public static Edge to(Node node) {
		return Edge.to(node);
	}
	
	public static Edge to(Target target) {
		return Edge.to(target);
	}
	
	public static PortNode portNode(String record) {
		return new PortNode().setRecord(record);
	}
	
	public static PortNode portNode(Compass position) {
		return new PortNode().setCompass(position);
	}
	
	public static PortNode portNode(String record, Compass position) {
		return new PortNode().setRecord(record).setCompass(position);
	}
	
	public static Attributed<?> nodeAttrs() {
		return Context.get().nodeAttrs();
	}
	
	public static Attributed<?> edgeAttrs() {
		return Context.get().edgeAttrs();
	}
	
	public static Attributed<?> graphAttrs() {
		return Context.get().graphAttrs();
	}
}
