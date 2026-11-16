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

        // Neither entry B nor C dominates D; this SCC has no natural-loop header.
        assertTrue(loops.isEmpty(), "Irreducible cycles must not be reported as natural loops");

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

    @Test
    void testLoopWithCrossEdges() {
        GraphTestBuilder builder = new GraphTestBuilder("""
            A -> B
            B -> C, D
            C -> E
            D -> E
            E -> F, G
            F -> B
            G -> C
            """);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree =
                new LT79Dom<>(builder.getGraph(), builder.getRoot());
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        // C does not dominate G: B -> D -> E -> G bypasses C.
        assertEquals(Set.of(builder.getVertex("B")), loops.keySet());
        assertEquals(Set.of(builder.getVertex("B"), builder.getVertex("C"),
                builder.getVertex("D"), builder.getVertex("E"), builder.getVertex("F"),
                builder.getVertex("G")), loops.get(builder.getVertex("B")));
        assertFalse(loops.get(builder.getVertex("B")).contains(builder.getVertex("A")));
    }

    @Test
    void nestingDepthCountsContainingLoopsWithoutRecursingOnItsOwnHeader() {
        GraphTestBuilder builder = new GraphTestBuilder("""
            A -> B
            B -> C, Exit
            C -> D
            D -> C, B
            """);
        LT79Dom<FakeFastVertex, FakeFastEdge> domTree =
                new LT79Dom<>(builder.getGraph(), builder.getRoot());
        Map<FakeFastVertex, Set<FakeFastVertex>> loops = domTree.findNaturalLoops();
        assertEquals(0, domTree.getLoopNestingDepth(builder.getVertex("A"), loops));
        assertEquals(0, domTree.getLoopNestingDepth(builder.getVertex("Exit"), loops));
        assertEquals(1, domTree.getLoopNestingDepth(builder.getVertex("B"), loops));
        assertEquals(2, domTree.getLoopNestingDepth(builder.getVertex("C"), loops));
        assertEquals(2, domTree.getLoopNestingDepth(builder.getVertex("D"), loops));
    }
}
