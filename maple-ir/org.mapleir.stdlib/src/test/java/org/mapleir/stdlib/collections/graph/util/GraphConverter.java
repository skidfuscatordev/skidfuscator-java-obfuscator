package org.mapleir.stdlib.collections.graph.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Node;
import org.mapleir.dot4j.model.PortNode;
import org.mapleir.dot4j.parse.Parser;
import org.mapleir.stdlib.collections.graph.FastGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.util.OrderedNode.OGraph;

public class GraphConverter {

	public static OGraph fromFile(String name) throws IOException {
		return (OGraph) fromDot(Parser.read(GraphConverter.class.getResourceAsStream(name)),
				(g) -> {
					if(g.isDirected()) {
						return new OrderedNode.ODirectedGraph();
					} else {
						return new OrderedNode.OUndirectedGraph();
					}
				},
				(n) -> {
					return new OrderedNode(Integer.parseInt(n.getName().toString()));
				},
				(src, dst) -> {
					return new OrderedNode.ONEdge(src, dst);
				});
	}
	
	public static <N extends FastGraphVertex, E extends FastGraphEdge<N>> FastGraph<N, E> fromDot(DotGraph graph,
			Function<DotGraph, FastGraph<N, E>> graphProducer, Function<Node, N> nodeProducer,
			BiFunction<N, N, E> edgeProducer) {
		FastGraph<N, E> fg = graphProducer.apply(graph);
		
		Map<Node, N> mapping = new HashMap<>();
		for(Node node : graph.getAllNodes()) {
			N fNode = nodeProducer.apply(node);
			mapping.put(node, fNode);
			fg.addVertex(fNode);
		}
		
		for(Node node : graph.getAllNodes()) {
			for(Edge e : node.getEdges()) {
				N src = mapping.get(node);
				N dst = mapping.get(((PortNode)e.getTarget()).getNode());
				fg.addEdge(edgeProducer.apply(src, dst));
			}
		}
		
		return fg;
	}
}
