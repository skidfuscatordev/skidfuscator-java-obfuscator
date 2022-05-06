package org.mapleir.dot4j.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.builtin.Label;

public class Serialiser {

	private final DotGraph graph;
	private final StringBuilder str;
	
	public Serialiser(DotGraph graph) {
		this.graph = graph;
		str = new StringBuilder();
	}
	
	public String serialise() {
		graph(graph, true);
		return str.toString();
	}
	
    private void graph(DotGraph graph, boolean toplevel) {
        graphInit(graph, toplevel);
        graphAttrs(graph);

        List<Node> nodes = new ArrayList<>();
        List<DotGraph> graphs = new ArrayList<>();
        Collection<Connected> linkables = linkedNodes(graph.nodes);
        linkables.addAll(linkedNodes(graph.subgraphs));
        for (Connected linkable : linkables) {
            if (linkable instanceof Node) {
                Node node = (Node) linkable;
                int i = indexOfName(nodes, node.name);
                if (i < 0) {
                    nodes.add(node);
                } else {
                    nodes.set(i, node.copy().merge(nodes.get(i)));
                }
            } else {
                graphs.add((DotGraph) linkable);
            }
        }

        nodes(graph, nodes);
        graphs(graphs, nodes);

        edges(nodes);
        edges(graphs);

        str.append('}');
    }

    private void graphAttrs(DotGraph graph) {
        attributes("graph", graph.graphAttrs);
        attributes("node", graph.nodeAttrs);
        attributes("edge", graph.edgeAttrs);
    }

    private void graphInit(DotGraph graph, boolean toplevel) {
        if (toplevel) {
            str.append(graph.strict ? "strict " : "").append(graph.directed ? "digraph " : "graph ");
            if (!graph.name.isEmpty()) {
                str.append(Label.of(graph.name).serialised()).append(' ');
            }
        } else if (!graph.name.isEmpty() || graph.cluster) {
            str.append("subgraph ")
                    .append(Label.of((graph.cluster ? "cluster_" : "") + graph.name).serialised())
                    .append(' ');
        }
        str.append("{\n");
    }

    private int indexOfName(List<Node> nodes, Label name) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private void attributes(String name, Attributed<?> attributed) {
        if (!attributed.isEmpty()) {
            str.append(name);
            attrs(attributed);
            str.append('\n');
        }
    }

    private Collection<Connected> linkedNodes(Collection<? extends Connected> nodes) {
        Set<Connected> visited = new LinkedHashSet<>();
        for (Connected node : nodes) {
            linkedNodes(node, visited);
        }
        return visited;
    }

    private void linkedNodes(Connected linkable, Set<Connected> visited) {
        if (!visited.contains(linkable)) {
            visited.add(linkable);
            for (Edge link : linkable.getEdges()) {
            	Target target = link.getTarget();
                if (target instanceof Node) {
                    linkedNodes((Node) target, visited);
                } else if (target instanceof PortNode) {
                    linkedNodes(((PortNode) target).node, visited);
                } else if (target instanceof DotGraph) {
                    linkedNodes((DotGraph) target, visited);
                } else {
                    throw new IllegalStateException("unexpected link to " + link.getTarget() + " of " + link.getTarget().getClass());
                }
            }
        }
    }

    private void nodes(DotGraph graph, List<Node> nodes) {
        for (Node node : nodes) {
            if (!node.attributes.isEmpty() || (graph.nodes.contains(node) && node.getEdges().isEmpty())) {
                node(node);
                str.append('\n');
            }
        }
    }

    private void graphs(List<DotGraph> graphs, List<Node> nodes) {
        for (DotGraph graph : graphs) {
            if (graph.getEdges().isEmpty() && !isLinked(graph, nodes) && !isLinked(graph, graphs)) {
                graph(graph, false);
                str.append('\n');
            }
        }
    }

    private boolean isLinked(DotGraph graph, List<? extends Connected> linkables) {
        for (Connected linkable : linkables) {
            for (Edge link : linkable.getEdges()) {
                if (link.getTarget().equals(graph)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void edges(List<? extends Connected> linkables) {
        for (Connected linkable : linkables) {
            for (Edge link : linkable.getEdges()) {
                linkTarget(link.getSource());
                str.append(graph.directed ? " -> " : " -- ");
                linkTarget(link.getTarget());
                attrs(link.getAttrs());
                str.append('\n');
            }
        }
    }

    private void linkTarget(Object linkable) {
        if (linkable instanceof Node) {
            node((Node) linkable);
        } else if (linkable instanceof PortNode) {
            port((PortNode) linkable);
        } else if (linkable instanceof DotGraph) {
            graph((DotGraph) linkable, false);
        } else {
            throw new IllegalStateException("unexpected link target " + linkable);
        }
    }

    private void node(Node node) {
        str.append(node.name.serialised());
        attrs(node.attributes);
    }

    private void port(PortNode portNode) {
        str.append(portNode.node.name.serialised());
        if (portNode.record != null) {
            str.append(':').append(Label.of(portNode.record).serialised());
        }
        if (portNode.getPosition() != null) {
            str.append(':').append(portNode.getPosition().value);
        }
    }

    private void attrs(Attributed<?> attrs) {
        if (!attrs.isEmpty()) {
            str.append(" [");
            boolean first = true;
            for (Entry<String, Object> attr : attrs) {
                if (first) {
                    first = false;
                } else {
                    str.append(',');
                }
                attr(attr.getKey(), attr.getValue());
            }
            str.append(']');
        }
    }

    private void attr(String key, Object value) {
        str.append(Label.of(key).serialised())
                .append('=')
                .append(Label.of(value).serialised());
    }
}
