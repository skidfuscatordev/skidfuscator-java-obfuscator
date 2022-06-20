package dev.skidfuscator.obfuscator.frame;

import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.ir.code.ExpressionPool;

public class FrameEdge implements FlowEdge<FrameNode> {
    private final FrameNode src;
    private final FrameNode dst;
    private final ExpressionPool frame;

    public FrameEdge(FrameNode src, FrameNode dst, ExpressionPool changes) {
        this.src = src;
        this.dst = dst;
        this.frame = changes;
    }

    @Override
    public FrameNode src() {
        return src;
    }

    @Override
    public FrameNode dst() {
        return dst;
    }

    public ExpressionPool frame() {
        return frame;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public String toGraphString() {
        return null;
    }

    @Override
    public String toInverseString() {
        return null;
    }

    @Override
    public FlowEdge<FrameNode> clone(FrameNode src, FrameNode dst) {
        return new FrameEdge(src, dst, frame);
    }
}
