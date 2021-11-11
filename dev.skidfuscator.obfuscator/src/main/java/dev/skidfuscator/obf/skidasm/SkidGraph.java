package dev.skidfuscator.obf.skidasm;

import dev.skidfuscator.obf.maple.FakeConditionalJumpStmt;
import dev.skidfuscator.obf.number.NumberManager;
import dev.skidfuscator.obf.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obf.number.hash.HashTransformer;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obf.utils.Blocks;
import dev.skidfuscator.obf.utils.RandomUtil;
import lombok.Getter;
import lombok.Setter;
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.*;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class SkidGraph {
    @Getter
    private final MethodNode node;
    private final SkidMethod method;

    @Setter
    @Getter
    private Local local;
    private final Map<BasicBlock, SkidBlock> cache = new HashMap<>();
    private final Set<LinearLink> linearLinks = new HashSet<>();

    public SkidGraph(MethodNode node, SkidMethod method) {
        this.node = node;
        this.method = method;
    }

    public void render(final ControlFlowGraph cfg) {
        if (cfg == null || cfg.vertices().size() == 0)
            return;

        // Phase 1: populate
        populate(cfg);

        final Local local = cfg.getLocals().get(cfg.getLocals().getMaxLocals() + 2);
        setLocal(local);
    }

    private void populate(final ControlFlowGraph cfg) {
        for (BasicBlock entry : cfg.getEntries()) {
            cache.put(entry, new SkidBlock(RandomUtil.nextInt(), entry));
        }
    }

    public void postlinearize(final ControlFlowGraph cfg) {
        if (cfg == null || cfg.vertices().size() == 0 || local == null)
            return;

        final Set<BasicBlock> toVisit = cfg.vertices()
                .stream()
                .filter(e -> cfg.getIncomingImmediate(e) == null)
                .collect(Collectors.toSet());

        // Phase 2
        linearize(cfg);

        range(cfg, local);
        linkage(cfg, local);

        /*BasicBlock next = cfg.verticesInOrder().iterator().next();
        while (true) {
            final BasicBlock immediate = cfg.getImmediate(next);

            if (immediate == null)
                break;

            final LinearLink linearLink = new LinearLink(next, immediate);

            if (linearLinks.contains(linearLink))
                continue;

            linearLinks.add(linearLink);

            // Add immediate seed translation
            addSeedToImmediate(local, next, immediate);

            next = immediate;
        }*/

        for (BasicBlock vertex : cfg.vertices()) {
            cfg.getEdges(vertex).stream()
                    .filter(e -> e instanceof ImmediateEdge)
                    .forEach(e -> {
                        addSeedToImmediate(local, e.src(), e.dst());
                    });
        }

        /*for (BasicBlock block : toVisit) {
            final Stack<BasicBlock> blocks = new Stack<>();
            blocks.add(block);

            while (!blocks.isEmpty()) {
                final BasicBlock stack = blocks.pop();

                if (stack == null || cfg.getEdges(stack).size() == 0)
                    continue;

                final BasicBlock immediate = cfg.getImmediate(stack);

                if (immediate == null)
                    continue;

                final LinearLink linearLink = new LinearLink(stack, immediate);

                if (linearLinks.contains(linearLink))
                    continue;

                linearLinks.add(linearLink);
                blocks.add(immediate);

                // Add immediate seed translation
                addSeedToImmediate(local, stack, immediate);
            }
        }*/

        for (BasicBlock block : cfg.vertices()) {
            final SkidBlock targetSeededBlock = getBlock(block);
            /*final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
            block.add(0, new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                    new ConstantExpr(block.getDisplayName() +" : c-var - begin : " + targetSeededBlock.getSeed())));
            final Local local2 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
            block.add(block.size() - 1, new CopyVarStmt(new VarExpr(local2, Type.getType(String.class)),
                    new ConstantExpr(block.getDisplayName() +" : c-var - end : " + targetSeededBlock.getSeed())));
            */
        }

    }

    private void linearize(final ControlFlowGraph cfg) {
        final BasicBlock entry = cfg.getEntries().iterator().next();
        final SkidBlock seedEntry = getBlock(entry);
        final Expr loadedChanged = /*new ConstantExpr(seedEntry.getSeed(), Type.INT_TYPE); */
                new XorNumberTransformer().getNumber(
                seedEntry.getSeed(),
                method.getSeed().getPrivate(),
                method.getSeed().getLocal()
        );
        final CopyVarStmt copyVarStmt = new CopyVarStmt(new VarExpr(local, Type.INT_TYPE), loadedChanged);
        entry.add(0, copyVarStmt);
    }

    private void linkage(final ControlFlowGraph cfg, final Local local) {
        for (BasicBlock vertex : cfg.vertices()) {
            new HashSet<>(vertex).stream().filter(e -> e instanceof SwitchStmt).forEach(e -> {
                addSeedToSwitch(local, vertex, (SwitchStmt) e);
            });
        }

        for (BasicBlock entry : cfg.vertices()) {
            new HashSet<>(entry).forEach(e -> {
                if (e instanceof UnconditionalJumpStmt) {
                    addSeedToUncJump(local, entry, (UnconditionalJumpStmt) e);
                } else if (e instanceof ConditionalJumpStmt && !(e instanceof FakeConditionalJumpStmt)) {
                    addSeedToCondJump(local, entry, (ConditionalJumpStmt) e);
                }
            });
        }
    }

    private void range(final ControlFlowGraph cfg, final Local local) {
        for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
            addSeedToRange(local, cfg, range);
        }
    }

    private void reset() {
        cache.clear();
    }

    private static class LinearLink {
        private final BasicBlock in;
        private final BasicBlock out;

        public LinearLink(BasicBlock in, BasicBlock out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LinearLink that = (LinearLink) o;

            if (in != null ? !in.equals(that.in) : that.in != null) return false;
            return out != null ? out.equals(that.out) : that.out == null;
        }

        @Override
        public int hashCode() {
            int result = in != null ? in.hashCode() : 0;
            result = 31 * result + (out != null ? out.hashCode() : 0);
            return result;
        }
    }

    public SkidBlock getBlock(final BasicBlock block) {
        SkidBlock seededBlock  = cache.get(block);
        if (seededBlock == null) {
            seededBlock = new SkidBlock(RandomUtil.nextInt(), block);
            cache.put(block, seededBlock);
        }

        return seededBlock;
    }

    private void addSeedToImmediate(final Local local, final BasicBlock block, final BasicBlock immediate) {
        final SkidBlock seededBlock = getBlock(block);
        final SkidBlock targetSeededBlock = getBlock(immediate);
        seededBlock.addSeedLoader(-1, local, seededBlock.getSeed(), targetSeededBlock.getSeed());
        /*final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
        block.add(block.size(), new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                new ConstantExpr(block.getDisplayName() +" : c-loc - immediate : " + targetSeededBlock.getSeed())));
        */
        // Ignore, this is for debugging
        /*
        final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
        block.add(new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                new ConstantExpr(block.getDisplayName() +" : Ivar: " + seededBlock.getSeed())));
        immediate.add(new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                new ConstantExpr(immediate.getDisplayName() +" : Ivar 2: " + targetSeededBlock.getSeed())));
                */
    }

    private void addSeedToUncJump(final Local local, final BasicBlock block, final UnconditionalJumpStmt stmt) {
        final int index = block.indexOf(stmt);
        final SkidBlock seededBlock = getBlock(block);
        final SkidBlock targetSeededBlock = getBlock(stmt.getTarget());
        seededBlock.addSeedLoader(index, local, seededBlock.getSeed(), targetSeededBlock.getSeed());

        /*final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
        block.add(index, new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                new ConstantExpr(block.getDisplayName() +" : c-loc - uncond : " + targetSeededBlock.getSeed())));
        */
        /*
        final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
        block.add(new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                new ConstantExpr(block.getDisplayName() +" : unc-var: " + targetSeededBlock.getSeed())));
                */
    }

    private void addSeedToCondJump(final Local local, final BasicBlock block, final ConditionalJumpStmt stmt) {
        //  Todo    Add support for various different types of conditional jumps
        //          support such as block splitting and shit to mess with reversers
        if (true) {
            final SkidBlock seededBlock = getBlock(block);
            final SkidBlock targetSeededBlock = getBlock(stmt.getTrueSuccessor());

            if (block.indexOf(stmt) < 0) {
                System.out.println("ISSUEEEEEE");
            }
            //final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
            seededBlock.addSeedLoader(block.indexOf(stmt), local, seededBlock.getSeed(), targetSeededBlock.getSeed());
            seededBlock.addSeedLoader(block.indexOf(stmt) + 1, local, targetSeededBlock.getSeed(), seededBlock.getSeed());

            /*final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
            block.add(block.indexOf(stmt) + 1, new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                    new ConstantExpr(block.getDisplayName() +" : c-loc - cond : " + targetSeededBlock.getSeed())));
            */

            /*block.add(block.indexOf(stmt), new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                    new ConstantExpr(block.getDisplayName() +" : c-var: " + targetSeededBlock.getSeed())));
            block.add(block.indexOf(stmt) + 1, new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                    new ConstantExpr(block.getDisplayName() +" : c-var: " + seededBlock.getSeed())));*/
            return;
        }

        final ConditionalJumpEdge<BasicBlock> edge = block.cfg.getEdges(block).stream()
                .filter(e -> !(e instanceof ImmediateEdge))
                .map(e -> (ConditionalJumpEdge<BasicBlock>) e)
                .filter(e -> e.dst().equals(stmt.getTrueSuccessor()))
                .findFirst()
                .orElse(null);

        block.cfg.removeEdge(edge);

        final SkidBlock seededBlock = getBlock(block);
        final BasicBlock target = stmt.getTrueSuccessor();
        final SkidBlock targetSeeded = getBlock(target);

        // Add jump and seed
        final BasicBlock basicBlock = new BasicBlock(block.cfg);
        final SkidBlock intraSeededBlock = getBlock(basicBlock);
        intraSeededBlock.addSeedLoader(0, local, seededBlock.getSeed(), targetSeeded.getSeed());
        basicBlock.add(new UnconditionalJumpStmt(target));

        // Add edge
        basicBlock.cfg.addVertex(basicBlock);
        basicBlock.cfg.addEdge(new UnconditionalJumpEdge<>(basicBlock, target));

        // Replace successor
        stmt.setTrueSuccessor(basicBlock);
        block.cfg.addEdge(new ConditionalJumpEdge<>(block, basicBlock, stmt.getOpcode()));
        //seededBlock.addSeedLoader(index + 2, local, targetSeededBlock.getSeed(), seededBlock.getSeed());
    }

    private void addSeedToSwitch(final Local local, final BasicBlock block, final SwitchStmt stmt) {
        final SkidBlock seededBlock = getBlock(block);

        for (BasicBlock value : stmt.getTargets().values()) {
            final SkidBlock target = getBlock(value);
            target.addSeedLoader(0, local, seededBlock.getSeed(), target.getSeed());

            /*final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
            value.add(0, new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                    new ConstantExpr(block.getDisplayName() +" : c-loc - switch : " + target.getSeed())));
            */
        }

        if (stmt.getDefaultTarget() == null || stmt.getDefaultTarget() == block)
            return;

        final SkidBlock dflt = getBlock(stmt.getDefaultTarget());
        dflt.addSeedLoader(0, local, seededBlock.getSeed(), dflt.getSeed());
    }

    private void addSeedToRange(final Local local, final ControlFlowGraph cfg, final ExceptionRange<BasicBlock> blockRange) {
        LinkedHashMap<Integer, BasicBlock> basicBlockMap = new LinkedHashMap<>();
        List<Integer> sortedList = new ArrayList<>();

        // Save current handler
        final BasicBlock basicHandler = blockRange.getHandler();
        final SkidBlock handler = getBlock(blockRange.getHandler());

        // Create new block handle
        final BasicBlock toppleHandler = new BasicBlock(cfg);
        cfg.addVertex(toppleHandler);
        blockRange.setHandler(toppleHandler);

        // Hasher
        final HashTransformer hashTransformer = new BitwiseHashTransformer();

        // For all block being read
        for (BasicBlock node : blockRange.getNodes()) {
            // Get their internal seed and add it to the list
            final SkidBlock internal = getBlock(node);
            sortedList.add(internal.getSeed());

            // Create a new switch block and get it's seeded variant
            final BasicBlock block = new BasicBlock(cfg);
            cfg.addVertex(block);
            final SkidBlock seededBlock = getBlock(block);

            // Add a seed loader for the incoming block and convert it to the handler's
            seededBlock.addSeedLoader(0, local, internal.getSeed(), handler.getSeed());

            // Jump to handler
            block.add(new UnconditionalJumpStmt(basicHandler));
            cfg.addEdge(new UnconditionalJumpEdge<>(block, basicHandler));

            // Final hashed
            final int hashed = hashTransformer.hash(internal.getSeed(), local).getHash();

            // Add to switch
            basicBlockMap.put(hashed, block);
            cfg.addEdge(new SwitchEdge<>(toppleHandler, block, hashed));

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
        for (int i = 0; i < 10; i++) {
            // Generate random seed + prevent conflict
            final int seed = RandomUtil.nextInt();
            if (sortedList.contains(seed))
                continue;

            // Add seed to list
            sortedList.add(seed);

            // Create new switch block
            final BasicBlock block = new BasicBlock(cfg);
            cfg.addVertex(block);

            // Get seeded version and add seed loader
            final SkidBlock seededBlock = getBlock(block);
            seededBlock.addSeedLoader(-1, local, seed, RandomUtil.nextInt());
            block.add(new UnconditionalJumpStmt(basicHandler));
            cfg.addEdge(new UnconditionalJumpEdge<>(block, basicHandler));

            basicBlockMap.put(seed, block);
            cfg.addEdge(new SwitchEdge<>(handler.getBlock(), block, seed));
        }

        // Hash
        final Expr hash = hashTransformer.hash(local);

        // Default switch edge
        final BasicBlock defaultBlock = Blocks.exception(cfg, "Error in hash");
        cfg.addEdge(new DefaultSwitchEdge<>(toppleHandler, defaultBlock));

        // Add switch
        // Todo     Add hashing to prevent dumb bs reversing
        final SwitchStmt stmt = new SwitchStmt(hash, basicBlockMap, defaultBlock);
        toppleHandler.add(stmt);

        // Add unconditional jump edge
        cfg.addEdge(new UnconditionalJumpEdge<>(toppleHandler, basicHandler));
    }

    public void cache(final BasicBlock basicBlock) {
        cache.put(basicBlock, new SkidBlock(RandomUtil.nextInt(), basicBlock));
    }

    public boolean isInit() {
        return node.node.name.equals("<init>");
    }
}
