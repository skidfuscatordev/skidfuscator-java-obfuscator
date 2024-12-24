package org.mapleir.stdlib.collections.graph.util;

import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdgeImpl;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.directed.FakeFastDirectedGraph;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Framework for creating test graphs from text representations.
 * Example usage:
 * <pre>
 * String graphText = """
 *     A -> B, C
 *     B -> C, D
 *     C -> B
 *     D -> A
 * """;
 * GraphTestBuilder builder = new GraphTestBuilder(graphText);
 * </pre>
 */
public class GraphTestBuilder {
    private final FastDirectedGraph<FakeFastVertex, FakeFastEdge> graph;
    private final Map<String, FakeFastVertex> vertices;
    private final FakeFastVertex root;

    /**
     * Creates a graph from a text representation.
     * Format:
     * NodeName -> Node1, Node2, Node3
     * First node is considered the root.
     */
    public GraphTestBuilder(String graphText) {
        graph = new FakeFastDirectedGraph();
        vertices = new HashMap<>();
        
        // Parse the graph text
        String[] lines = graphText.trim().split("\n");
        Pattern pattern = Pattern.compile("(\\w+)\\s*->\\s*([\\w\\s,]+)");

        FakeFastVertex firstVertex = null;
        
        for (String line : lines.clone()) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid line format: " + line);
            }
            
            String sourceName = matcher.group(1);
            String[] targetNames = matcher.group(2).split("\\s*,\\s*");

            FakeFastVertex source = getOrCreateVertex(sourceName);
            if (firstVertex == null) {
                firstVertex = source;
            }
            
            for (String targetName : targetNames) {
                FakeFastVertex target = getOrCreateVertex(targetName);
                graph.addEdge(new FakeFastEdge(source, target, true));
            }
        }
        
        this.root = firstVertex;
        if (this.root == null) {
            throw new IllegalArgumentException("Graph must have at least one vertex");
        }
    }

    private FakeFastVertex getOrCreateVertex(String name) {
        return vertices.computeIfAbsent(name, n -> {
            FakeFastVertex v = new FakeFastVertex(n);
            graph.addVertex(v);
            return v;
        });
    }

    public FastDirectedGraph<FakeFastVertex, FakeFastEdge> getGraph() {
        return graph;
    }

    public FakeFastVertex getRoot() {
        return root;
    }

    public FakeFastVertex getVertex(String name) {
        return vertices.get(name);
    }
}