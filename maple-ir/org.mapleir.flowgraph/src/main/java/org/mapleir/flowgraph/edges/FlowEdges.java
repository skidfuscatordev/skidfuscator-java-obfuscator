package org.mapleir.flowgraph.edges;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public interface FlowEdges {

	int IMMEDIATE = 1;
	int UNCOND = 2;
	int COND = 3;
	int TRYCATCH = 4;
	int SWITCH = 5;
	int DEFAULT_SWITCH = 6;
	int DUMMY = 7;
	
	static <N extends FastGraphVertex> FlowEdge<N> mock(FlowEdge<?> e, N src, N dst) {
		switch(e.getType()) {
			case IMMEDIATE:
				return new ImmediateEdge<>(src, dst);
			case UNCOND:
				return new UnconditionalJumpEdge<>(src, dst);
			case COND:
				return new ConditionalJumpEdge<>(src, dst, ((ConditionalJumpEdge<?>)e).opcode);
			case TRYCATCH:
				return new TryCatchEdge<>(src, dst);
			case SWITCH: {
				SwitchEdge<?> sw = (SwitchEdge<?>) e;
				return new SwitchEdge<>(src, dst, sw.value);
			}
			case DEFAULT_SWITCH: {
				DefaultSwitchEdge<?> sw = (DefaultSwitchEdge<?>) e;
				return new DefaultSwitchEdge<>(src, dst);
			}
			case DUMMY:
				return new DummyEdge<>(src, dst);
			default:
				throw new UnsupportedOperationException(e + ", " + e.getType());
		}
	}
}
