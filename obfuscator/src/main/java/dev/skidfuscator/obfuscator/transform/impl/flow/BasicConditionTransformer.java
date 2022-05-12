package dev.skidfuscator.obfuscator.transform.impl.flow;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
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

public class BasicConditionTransformer extends AbstractTransformer {
    public BasicConditionTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public BasicConditionTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"Basic Condition Transformer", children);
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        if (methodNode.isAbstract() || methodNode.isInit())
            return;

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        for (BasicBlock parent : new HashSet<>(cfg.vertices())) {
            if (parent.size() == 0)
                continue;

            final Stmt stmt = parent.get(parent.size() - 1);

            if (!(stmt instanceof ConditionalJumpStmt)) {
                continue;
            }

            final ConditionalJumpEdge<BasicBlock> edge = cfg
                    .getEdges(parent)
                    .stream()
                    .filter(e -> e instanceof ConditionalJumpEdge)
                    .map(e -> (ConditionalJumpEdge) e)
                    .filter(e -> e.dst() == ((ConditionalJumpStmt) stmt).getTrueSuccessor())
                    .findFirst()
                    .orElse(null);

            if (edge == null)
                continue;

            final ConditionalJumpStmt jump = (ConditionalJumpStmt) stmt;
            final BasicBlock target = jump.getTrueSuccessor();

            final SkidBlock basicBlock = new SkidBlock(cfg);
            cfg.addVertex(basicBlock);

            final HashTransformer transformer = NumberManager.randomHasher();
            final SkiddedHash hash = transformer.hash(
                    methodNode.getBlockPredicate(basicBlock),
                    cfg,
                    methodNode.getFlowPredicate().getGetter()
            );

            final ConditionalJumpStmt conditionalJumpStmt = new ConditionalJumpStmt(
                    hash.getExpr(),
                    new ConstantExpr(hash.getHash(), Type.INT_TYPE),
                    target,
                    ConditionalJumpStmt.ComparisonType.EQ
            );
            basicBlock.add(conditionalJumpStmt);
            cfg.addEdge(new ConditionalJumpEdge<>(
                    basicBlock,
                    target,
                    conditionalJumpStmt.getOpcode()
            ));

            // Replace the edge
            cfg.removeEdge(edge);
            cfg.addEdge(new ConditionalJumpEdge<>(
                    edge.src(),
                    basicBlock,
                    edge.opcode
            ));

            // Exception
            final BasicBlock exception = Blocks.exception(cfg);

            // Add gay loop
            final UnconditionalJumpEdge<BasicBlock> edge1 = new UnconditionalJumpEdge<>(
                    basicBlock,
                    exception
            );
            basicBlock.add(new UnconditionalJumpStmt(
                    exception,
                    edge1
            ));
            cfg.addEdge(edge1);

            jump.setTrueSuccessor(basicBlock);

            event.tick();
        }
    }
}
