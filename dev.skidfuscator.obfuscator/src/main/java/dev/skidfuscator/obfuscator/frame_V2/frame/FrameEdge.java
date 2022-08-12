package dev.skidfuscator.obfuscator.frame_V2.frame;

import org.mapleir.flowgraph.edges.FlowEdge;
import dev.skidfuscator.obfuscator.frame_V2.frame.type.TypeHeader;

public class FrameEdge implements FlowEdge<FrameNode> {
    private final FrameNode src;
    private final FrameNode dst;
    private final TypeHeader frame;

    public FrameEdge(FrameNode src, FrameNode dst, TypeHeader changes) {
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

    public TypeHeader frame() {
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
