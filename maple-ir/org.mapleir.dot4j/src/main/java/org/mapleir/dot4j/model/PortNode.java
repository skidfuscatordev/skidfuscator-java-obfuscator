package org.mapleir.dot4j.model;

public class PortNode implements Target, Source<Node> {

	protected Node node;
	protected String record;
	protected Compass position;
	
	public PortNode() {
	}
	
	public PortNode(Node node, String record, Compass position) {
		this.node = node;
		this.record = record;
		this.position = position;
	}
	
	public PortNode copy() {
		return new PortNode(node, record, position);
	}
	
	public PortNode setNode(Node node) {
		this.node = node;
		return this;
	}
	
	public PortNode setRecord(String record) {
		this.record = record;
		return this;
	}
	
	public PortNode setCompass(Compass position) {
		this.position = position;
		return this;
	}
	
	public Node getNode() {
		return node;
	}
	
	public String getRecord() {
		return record;
	}
	
	public Compass getPosition() {
		return position;
	}
	
	@Override
	public Node addEdge(Target target) {
		return node.addEdge(target);
	}
	
	@Override
	public Edge edgeToHere() {
		return Edge.to(this);
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o == null || getClass() != o.getClass()) {
			return false;
		}
		PortNode n = (PortNode) o;
        if (node != null ? !node.equals(n.node) : n.node != null) {
            return false;
        }
        if (record != null ? !record.equals(n.record) : n.record != null) {
            return false;
        }
        return position == n.position;
	}
	
	@Override
	public int hashCode() {
		int result = node != null ? node.hashCode() : 0;
		result = 31 * result + (record != null ? record.hashCode() : 0);
		result = 31 * result + (position != null ? position.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return (record == null ? "" : record) + ":" + (position == null ? "" : position) + ":" + node.toString();
	}
}
