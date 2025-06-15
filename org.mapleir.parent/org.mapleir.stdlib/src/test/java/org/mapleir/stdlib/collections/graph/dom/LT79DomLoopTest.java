package org.mapleir.stdlib.collections.graph.dom;

import org.junit.jupiter.api.Test;
import org.mapleir.stdlib.collections.graph.algorithms.LT79Dom;
import org.mapleir.stdlib.collections.graph.util.FakeFastEdge;
import org.mapleir.stdlib.collections.graph.util.FakeFastVertex;
import org.mapleir.stdlib.collections.graph.util.GraphTestBuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;

public class LT79DomLoopTest {

    @Test
    public void testSimpleLoop() {
        String graphText = """
            A -> B
            B -> C
            C -> B
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        
        assertEquals(1, loops.size(), "Should find one loop");
        assertTrue(loops.containsKey(builder.getVertex("B")), "B should be loop header");
        assertTrue(loops.get(builder.getVertex("B")).containsAll(
            Set.of(builder.getVertex("B"), builder.getVertex("C"))),
            "Loop should contain B and C");
    }

    @Test
    public void testNestedLoops() {
        String graphText = """
            A -> B
            B -> C
            C -> D
            D -> B
            D -> E
            E -> B
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        
        assertEquals(1, loops.size(), "Should find one loop");
        assertTrue(loops.containsKey(builder.getVertex("B")), "B should be loop header");
        
        Set<FakeFastVertex> loopBody = loops.get(builder.getVertex("B"));
        assertTrue(loopBody.containsAll(Set.of(
            builder.getVertex("B"),
            builder.getVertex("C"),
            builder.getVertex("D"),
            builder.getVertex("E")
        )), "Loop should contain B, C, D, and E");
    }

    @Test
    public void testSelfLoop() {
        String graphText = """
            A -> B
            B -> B
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        
        assertEquals(1, loops.size(), "Should find one loop");
        assertTrue(loops.containsKey(builder.getVertex("B")), "B should be loop header");
        assertEquals(
            Set.of(builder.getVertex("B")),
            loops.get(builder.getVertex("B")),
            "Self loop should contain only B"
        );
    }

    @Test
    public void testMultipleExitLoops() {
        String graphText = """
            A -> B
            B -> C, E
            C -> D
            D -> B, F
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        
        assertTrue(loops.containsKey(builder.getVertex("B")), "B should be loop header");
        Set<FakeFastVertex> loopBody = loops.get(builder.getVertex("B"));
        assertTrue(loopBody.containsAll(Set.of(
            builder.getVertex("B"),
            builder.getVertex("C"),
            builder.getVertex("D")
        )), "Loop should contain B, C, and D");
        assertFalse(loopBody.contains(builder.getVertex("E")), "Loop should not contain exit node E");
        assertFalse(loopBody.contains(builder.getVertex("F")), "Loop should not contain exit node F");
    }

    @Test
    public void testComplexNestedLoops() {
        String graphText = """
            A -> B
            B -> C
            C -> D, G
            D -> E
            E -> F
            F -> D
            G -> H
            H -> G, B
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        
        assertEquals(3, loops.size(), "Should find three loops");
        assertTrue(loops.containsKey(builder.getVertex("B")), "B should be loop header");
        assertTrue(loops.containsKey(builder.getVertex("D")), "D should be loop header");
        assertTrue(loops.containsKey(builder.getVertex("G")), "G should be loop header");
    }

    @Test
    void testIrreducibleLoop() {
        String graphText = """
            A -> B, C
            B -> D
            C -> D
            D -> B, C
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();

        loops.forEach((k, v) -> System.out.println(k + " -> " + v.stream().map(FakeFastVertex::toString).reduce("", (a, b) -> a + " " + b)));
        
        // Should identify both B and C as loop headers since both are entry points
        assertTrue(loops.containsKey(builder.getVertex("B")), "B should be loop header");
        assertTrue(loops.containsKey(builder.getVertex("D")), "C should be loop header");
        
        // Both loops should contain D as it's part of both cycles
        assertTrue(loops.get(builder.getVertex("B")).contains(builder.getVertex("D")), 
            "B's loop should contain D");
        assertTrue(loops.get(builder.getVertex("D")).contains(builder.getVertex("D")),
            "D's loop should contain D");
    }

    @Test
    void testComplexLoopNesting() {
        String graphText = """
            Entry -> A
            A -> B
            B -> C, Exit1
            C -> D, Exit2
            D -> E, F
            E -> C
            F -> G
            G -> F, B, Exit3
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        
        // Should find three nested loops: B->G->B, C->E->C, and F->G->F
        assertEquals(3, loops.size(), "Should find three loops");
        
        // Check outer loop (B)
        Set<FakeFastVertex> outerLoop = loops.get(builder.getVertex("B"));
        assertNotNull(outerLoop, "B should be a loop header");
        assertTrue(outerLoop.containsAll(Set.of(
            builder.getVertex("B"),
            builder.getVertex("C"),
            builder.getVertex("D"),
            builder.getVertex("E"),
            builder.getVertex("F"),
            builder.getVertex("G")
        )), "Outer loop should contain all inner loop nodes");
        
        // Check middle loop (C)
        Set<FakeFastVertex> middleLoop = loops.get(builder.getVertex("C"));
        assertNotNull(middleLoop, "C should be a loop header");
        assertTrue(middleLoop.containsAll(Set.of(
            builder.getVertex("C"),
            builder.getVertex("D"),
            builder.getVertex("E")
        )), "Middle loop should contain C, D, and E");
        
        // Check inner loop (F)
        Set<FakeFastVertex> innerLoop = loops.get(builder.getVertex("F"));
        assertNotNull(innerLoop, "F should be a loop header");
        assertTrue(innerLoop.containsAll(Set.of(
            builder.getVertex("F"),
            builder.getVertex("G")
        )), "Inner loop should contain F and G");
    }

    @Test
    void testLoopWithMultipleBackEdges() {
        String graphText = """
            A -> B
            B -> C
            C -> D
            D -> E
            E -> B, C, D
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        
        // Should find three loops due to multiple back edges from E
        assertEquals(3, loops.size(), "Should find three loops");
        
        // Check each loop
        Set<FakeFastVertex> loopB = loops.get(builder.getVertex("B"));
        Set<FakeFastVertex> loopC = loops.get(builder.getVertex("C"));
        Set<FakeFastVertex> loopD = loops.get(builder.getVertex("D"));
        
        assertNotNull(loopB, "B should be a loop header");
        assertNotNull(loopC, "C should be a loop header");
        assertNotNull(loopD, "D should be a loop header");
        
        // Check loop contents
        assertTrue(loopB.containsAll(Set.of(
            builder.getVertex("B"),
            builder.getVertex("C"),
            builder.getVertex("D"),
            builder.getVertex("E")
        )), "B's loop should contain all nodes");
        
        assertTrue(loopC.containsAll(Set.of(
            builder.getVertex("C"),
            builder.getVertex("D"),
            builder.getVertex("E")
        )), "C's loop should contain C, D, and E");
        
        assertTrue(loopD.containsAll(Set.of(
            builder.getVertex("D"),
            builder.getVertex("E")
        )), "D's loop should contain D and E");
    }

    @Test
    void testDisconnectedLoops() {
        String graphText = """
            A -> B, X
            B -> C
            C -> B
            X -> Y
            Y -> Z
            Z -> Y
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        
        // Should find two separate loops
        assertEquals(2, loops.size(), "Should find two loops");
        
        // Check first loop
        Set<FakeFastVertex> loopB = loops.get(builder.getVertex("B"));
        assertNotNull(loopB, "B should be a loop header");
        assertTrue(loopB.containsAll(Set.of(
            builder.getVertex("B"),
            builder.getVertex("C")
        )), "First loop should contain B and C");
        
        // Check second loop
        Set<FakeFastVertex> loopY = loops.get(builder.getVertex("Y"));
        assertNotNull(loopY, "Y should be a loop header");
        assertTrue(loopY.containsAll(Set.of(
            builder.getVertex("Y"),
            builder.getVertex("Z")
        )), "Second loop should contain Y and Z");
    }

    /**
     * Example graph structure showing natural loops:
     *
     *           A
     *           |
     *           v
     *     +---> B <---+
     *     |     | \   |
     *     |     v  v  |
     *     |     C   D |
     *     |     |   | |
     *     |     v   v |
     *     |     E ----+
     *     |    / \
     *     F <-'   v
     *             G
     *             |
     *             +-> C
     *
     * Contains two natural loops:
     * 1. B -> F -> B (outer loop)
     * 2. C -> G -> C (inner loop)
     */
    @Test
    void testLoopWithCrossEdges() {
        String graphText = """
            A -> B
            B -> C, D
            C -> E
            D -> E
            E -> F, G
            F -> B
            G -> C
            """;
        
        GraphTestBuilder builder = new GraphTestBuilder(graphText);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree = 
            new LT79Dom<>(builder.getGraph(), builder.getRoot());
        
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();

        loops.forEach((k, v) -> System.out.println(k + " -> " + v.stream().map(FakeFastVertex::toString).reduce("", (a, b) -> a + " " + b)));
        // Should find two interleaved loops
        assertEquals(2, loops.size(), "Should find two loops: B->F and C->G");
        
        Set<FakeFastVertex> loopB = loops.get(builder.getVertex("B"));
        Set<FakeFastVertex> loopC = loops.get(builder.getVertex("E"));
        
        assertNotNull(loopB, "B should be a loop header");
        assertNotNull(loopC, "E should be a loop header");
        
        // Check that cross edges are handled correctly
        assertTrue(loopB.containsAll(Set.of(
            builder.getVertex("B"),
            builder.getVertex("C"),
            builder.getVertex("D"),
            builder.getVertex("E"),
            builder.getVertex("F")
        )), "B's loop should contain all nodes except G");
        
        assertTrue(loopC.containsAll(Set.of(
            builder.getVertex("C"),
            builder.getVertex("E"),
            builder.getVertex("G")
        )), "E's loop should contain C, E, and G");
    }
}