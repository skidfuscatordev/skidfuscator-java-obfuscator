package dev.skidfuscator.obfuscator.predicate.renderer.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obfuscator.predicate.renderer.InstructionRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.seed.SeedLoadable;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeUnconditionalJumpStmt;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.DefaultSwitchEdge;
import org.mapleir.flowgraph.edges.SwitchEdge;
import org.mapleir.flowgraph.edges.TryCatchEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;

import java.util.*;

public class ExceptionRenderer implements InstructionRenderer<ExceptionRange<BasicBlock>>, SeedLoadable {
    @Override
    public void transform(
            final Skidfuscator base,
            final ControlFlowGraph cfg,
            final ExceptionRange<BasicBlock> blockRange) {
        final SkidMethodNode methodNode = (SkidMethodNode) cfg.getMethodNode();

        LinkedHashMap<Integer, BasicBlock> basicBlockMap = new LinkedHashMap<>();
        List<Integer> sortedList = new ArrayList<>();

        // Save current handler
        final BasicBlock basicHandler = blockRange.getHandler();
        final SkidBlock handler = (SkidBlock) blockRange.getHandler();

        // Create new block handle
        final BasicBlock toppleHandler = new SkidBlock(cfg);
        cfg.addVertex(toppleHandler);
        blockRange.setHandler(toppleHandler);

        // Hasher
        final HashTransformer hashTransformer = base.getBitwiseHasher();

        // Add support for duplicate keys
        final Set<Integer> calledHashes = new HashSet<>();

        // For all block being read
        for (BasicBlock node : blockRange.getNodes()) {
            if (node instanceof FakeBlock)
                continue;

            // Get their internal seed and add it to the list
            final SkidBlock internal = (SkidBlock) node;

            // Check if key is already generated
            if (calledHashes.contains(internal.getSeed()))
                continue;
            calledHashes.add(internal.getSeed());

            // Create a new switch block and get it's seeded variant
            final SkidBlock block = new SkidBlock(cfg);
            block.setFlag(SkidBlock.FLAG_PROXY, true);
            methodNode.getFlowPredicate().set(block, methodNode.getBlockPredicate(internal));

            cfg.addVertex(block);

            // Jump to handler
            final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(
                    block,
                    handler
            );
            final UnconditionalJumpStmt proxy = new FakeUnconditionalJumpStmt(handler, edge);
            proxy.setFlag(SkidBlock.FLAG_PROXY, true);
            block.add(proxy);

            // Add a seed loader for the incoming block and convert it to the handler's
            this.addSeedLoader(
                    methodNode,
                    block,
                    handler,
                    0,
                    methodNode.getFlowPredicate(),
                    methodNode.getBlockPredicate(internal),
                    "Exception Range " + Arrays.toString(blockRange.getTypes().toArray())
            );

            cfg.addEdge(edge);

            // Final hashed
            final int hashed = hashTransformer.hash(
                    methodNode.getBlockPredicate(internal),
                    internal,
                    methodNode.getFlowPredicate().getGetter()
            ).getHash();

            // Add to switch
            // TODO: Revert back to hashed
            basicBlockMap.put(hashed, block);
            cfg.addEdge(new SwitchEdge<>(toppleHandler, block, hashed));
            sortedList.add(hashed);

            // Find egde and transform
            cfg.getEdges(node)
                    .stream()
                    .filter(e -> e instanceof TryCatchEdge)
                    .map(e -> (TryCatchEdge<BasicBlock>) e)
                    .filter(e -> e.erange == blockRange)
                    .findFirst()
                    .ifPresent(cfg::removeEdge);

            // Add new edge
            cfg.addEdge(new TryCatchEdge<>(node, blockRange));
        }

        // Haha get fucked
        // Todo     Fix the other shit to re-enable this; this is for the lil shits
        //          (love y'all tho) that are gonna try reversing this
            /*for (int i = 0; i < 10; i++) {
                // Generate random seed + prevent conflict
                final int seed = RandomUtil.nextInt();
                if (sortedList.contains(seed))
                    continue;

                // Add seed to list
                sortedList.add(seed);

                // Create new switch block
                final SkidBlock block = new SkidBlock(cfg);
                cfg.addVertex(block);

                // Get seeded version and add seed loader
                addSeedLoader(

                );
                seededBlock.addSeedLoader(-1, local, seed, RandomUtil.nextInt());
                block.add(new UnconditionalJumpStmt(basicHandler));
                cfg.addEdge(new UnconditionalJumpEdge<>(block, basicHandler));

                basicBlockMap.put(seed, block);
                cfg.addEdge(new SwitchEdge<>(handler.getBlock(), block, seed));
            }*/

        // Hash
        final Expr hash = hashTransformer.hash(toppleHandler, methodNode.getFlowPredicate().getGetter());

        // Default switch edge
        final BasicBlock defaultBlock = Blocks.exception(cfg, "Error in hash");
        cfg.addEdge(new DefaultSwitchEdge<>(toppleHandler, defaultBlock));

        // Add switch
        final SwitchStmt stmt = new SwitchStmt(hash, basicBlockMap, defaultBlock);
        toppleHandler.add(stmt);

        // Add unconditional jump edge
        cfg.addEdge(new UnconditionalJumpEdge<>(toppleHandler, basicHandler));
    }
}
