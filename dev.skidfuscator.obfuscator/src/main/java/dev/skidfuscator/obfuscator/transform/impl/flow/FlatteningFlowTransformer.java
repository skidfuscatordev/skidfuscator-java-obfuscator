package dev.skidfuscator.obfuscator.transform.impl.flow;

import com.google.errorprone.annotations.Var;
import com.google.gson.internal.Streams;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.FinalMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.PreMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.OpcodeUtil;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.*;
import org.mapleir.ir.algorithms.DominanceLivenessAnalyser;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.CastExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.impl.BasicLocal;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FlatteningFlowTransformer extends AbstractTransformer {
    public FlatteningFlowTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "Control Flow Flattening");
    }

    @Listen
    void handle(final InitMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        final ControlFlowGraph cfg = methodNode.getCfg();
        /*for (BasicBlock block : new ArrayList<>(cfg.vertices())) {
            final BasicBlock immediate = cfg.getImmediate(block);

            if (immediate == null)
                continue;

            cfg.removeEdge(cfg.getImmediateEdge(block));

            final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(block, immediate);
            cfg.addEdge(edge);
            block.add(new UnconditionalJumpStmt(immediate, edge));
        }*/
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        final Skidfuscator skidfuscator = event.getSkidfuscator();

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        if (cfg.size() <= 3) {
            return;
        }

        if (methodNode.isAbstract() || methodNode.isNative())
            return;

        final BlockOpaquePredicate opaquePredicate = methodNode.getFlowPredicate();

        assert opaquePredicate != null : "Flow Predicate is null?";
        final PredicateFlowGetter getter = opaquePredicate.getGetter();

        // TODO: Figure why this is happening
        if (getter == null)
            return;

        final Set<BasicBlock> exempt = new HashSet<>();
        for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
            exempt.addAll(range.getNodes());
            exempt.add(range.getHandler());
        }

        /*for (BasicBlock vertex : cfg.vertices()) {
            if (vertex.getStack() == null)
                exempt.add(vertex);
        }*/

        if (cfg.vertices()
                .stream()
                .filter(e -> !exempt.contains(e) && !e.isEmpty())
                .flatMap(Collection::stream)
                .noneMatch(e -> e instanceof ConditionalJumpStmt))
            return;

        final LinkedHashMap<Integer, BasicBlock> destinations = new LinkedHashMap<>();
        final SkidBlock dispatcherBlock = new SkidBlock(cfg);
        cfg.addVertex(dispatcherBlock);

        /* Another failed attempt at computing a local range */
        /*final Set<Integer> locals = new HashSet<>();
        final Map<Integer, Type> localsVisited = new HashMap<>();
        final Map<Integer, Map<Type, Set<CopyVarStmt>>> cache = new HashMap<>();

        for (Stmt stmt : cfg.stmts()) {
            if (!(stmt instanceof CopyVarStmt))
                continue;

            final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;

            cache.computeIfAbsent(copyVarStmt.getIndex(), e -> new HashMap<>())
                    .computeIfAbsent(copyVarStmt.getType(), e -> new HashSet<>())
                    .add(copyVarStmt);

            if (locals.contains(copyVarStmt.getIndex()))
                continue;

            Type type = localsVisited.computeIfAbsent(copyVarStmt.getIndex(), e -> copyVarStmt.getType());
            if (!type.equals(copyVarStmt.getType())) {
                locals.add(copyVarStmt.getIndex());
            }
        }

        for (Integer local : locals) {
            Map<Type, Set<CopyVarStmt>> var = cache.get(local);


            var.forEach((type, set) -> {

                final Set<VarExpr> exprs = cfg.allExprStream()
                        .filter(VarExpr.class::isInstance)
                        .map(VarExpr.class::cast)
                        .filter(e -> !(e.getRootParent() instanceof CopyVarStmt))
                        .filter(e -> e.getIndex() == local && type.equals(e.getType()))
                        .collect(Collectors.toSet());

                final List<BasicBlock> visited = new ArrayList<>();
                final List<BasicBlock> blocks = new ArrayList<>();


                for (VarExpr varExpr : exprs) {
                    blocks.add(varExpr.getBlock());
                }

                final Stack<BasicBlock> stack = new Stack<>();
                for (CopyVarStmt copyVarStmt : set) {
                    if (visited.contains(copyVarStmt.getBlock()))
                        return;

                    stack.addAll(cfg.getSuccessors(copyVarStmt.getBlock()).collect(Collectors.toList()));
                    visited.add(copyVarStmt.getBlock());
                }

                while (!stack.isEmpty()) {
                    if (blocks.isEmpty())
                        break;

                    final BasicBlock popped = stack.pop();

                    if (visited.contains(popped))
                        continue;

                    popped.add(
                            0,
                            new CopyVarStmt(
                                    new VarExpr(cfg.getLocals().get(local), type),
                                    new CastExpr(new VarExpr(cfg.getLocals().get(local), type), type)
                            )
                    );

                    blocks.remove(popped);
                    exempt.add(popped);
                    visited.add(popped);
                    stack.addAll(
                            cfg.getSuccessors(popped)
                                    .collect(Collectors.toList())
                    );
                }
            });
        }*/

        /* Failed attempt at exempting trouble making locals */
        /*for (Integer r : locals) {
            for (VarExpr varExpr : localMap.get(r)) {
                final CastExpr castExpr = new CastExpr(new ConstantExpr(""), varExpr.getType());
                varExpr.getParent().overwrite(varExpr, castExpr);
                varExpr.setParent(null);
                castExpr.setExpression(varExpr);

                varExpr.getLocal().setType(TypeUtil.OBJECT_TYPE);
                exempt.add(varExpr.getBlock());
            }

            cfg.getLocals().defs.put()
        }*/

        new HashSet<>(cfg.vertices())
                .stream()
                .filter(e -> !exempt.contains(e) && !e.isEmpty())
                .flatMap(Collection::stream)
                .filter(e -> e instanceof ConditionalJumpStmt)
                .map(e -> (ConditionalJumpStmt) e)
                .forEach(e -> {
                    final SkidBlock currentBlock = (SkidBlock) e.getBlock();
                    final SkidBlock oldTarget = (SkidBlock) e.getTrueSuccessor();

                    /* Replace target block */
                    e.setTrueSuccessor(dispatcherBlock);

                    /* Replace edge */
                    final ConditionalJumpEdge<BasicBlock> edge = new ConditionalJumpEdge<>(
                            currentBlock,
                            dispatcherBlock,
                            e.getEdge() == null ? Opcodes.IFEQ : e.getEdge().opcode
                    );

                    if (e.getEdge() != null)
                        cfg.removeEdge(e.getEdge());
                    cfg.addEdge(edge);
                    e.setEdge(edge);

                    final int seed = methodNode.getBlockPredicate(oldTarget);
                    destinations.put(seed, oldTarget);
                    cfg.addEdge(new SwitchEdge<>(dispatcherBlock, oldTarget, seed));
                });

        // We put it here to prevent adding a dead block

        final BasicBlock fuck = Blocks.exception(cfg, "We messed up bogo...");
        dispatcherBlock.add(new SwitchStmt(
                getter.get(dispatcherBlock),
                destinations,
                fuck
        ));

        cfg.addEdge(new DefaultSwitchEdge<>(dispatcherBlock, fuck));

        /*if (IntegerBlockPredicateRenderer.DEBUG) {
            methodNode.dump();

            try {
                Analyzer<?> analyzer = new Analyzer<>(new SimpleVerifier());
                analyzer.analyzeAndComputeMaxs(methodNode.owner.getName(), methodNode.node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
    }
}
