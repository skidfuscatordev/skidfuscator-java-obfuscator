package dev.skidfuscator.obf.maple;

import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class FakeConditionalJumpEdge<N extends FastGraphVertex> extends ConditionalJumpEdge<N> {
    public FakeConditionalJumpEdge(N src, N dst, int opcode) {
        super(src, dst, opcode);
    }
}
