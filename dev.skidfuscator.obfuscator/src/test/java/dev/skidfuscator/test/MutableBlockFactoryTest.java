package dev.skidfuscator.test;

import org.junit.jupiter.api.Test;
import org.mapleir.asm.MethodNode;
import org.mapleir.context.IRCache;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.DefaultBlockFactory;
import org.mapleir.ir.cfg.builder.ssa.BlockBuilder;
import org.mapleir.ir.cfg.SSAFactory;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.stdlib.collections.map.CachedKeyedValueCreator;

public class MutableBlockFactoryTest {

    @Test
    public void attemptMethodNode() {
        SSAFactory seededFactory = new DefaultBlockFactory() {
            @Override
            public BlockBuilder block() {
                return new BlockBuilder() {
                    private ControlFlowGraph graph;

                    @Override
                    public BlockBuilder cfg(ControlFlowGraph cfg) {
                        this.graph = cfg;
                        return this;
                    }

                    @Override
                    public BasicBlock build() {
                        assert graph != null : "ControlFlowGraph must not be null";
                        return new ExternBlock(null);
                    }
                };
            }
        };
        IRCache irCache = new IRCache(new CachedKeyedValueCreator<MethodNode, ControlFlowGraph>() {
            @Override
            protected ControlFlowGraph create0(MethodNode methodNode) {
                return ControlFlowGraphBuilder.build(methodNode, seededFactory);
            }
        });

        // Todo generate a sample class and test the block building
    }

    private static class ExternBlock extends BasicBlock {
        public ExternBlock(ControlFlowGraph cfg) {
            super(cfg);
        }
    }
}
