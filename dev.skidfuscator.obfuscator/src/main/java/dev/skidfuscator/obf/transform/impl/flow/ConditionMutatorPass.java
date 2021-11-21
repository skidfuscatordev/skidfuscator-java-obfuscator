package dev.skidfuscator.obf.transform.impl.flow;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.maple.FakeArithmeticExpr;
import dev.skidfuscator.obf.maple.FakeConditionalJumpStmt;
import dev.skidfuscator.obf.number.NumberManager;
import dev.skidfuscator.obf.number.hash.HashTransformer;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obf.skidasm.SkidBlock;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import dev.skidfuscator.obf.utils.Blocks;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.ImmediateEdge;
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
import org.mapleir.ir.utils.CFGUtils;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConditionMutatorPass implements FlowPass {
    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract() || methodNode.isInit())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

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

                final BasicBlock basicBlock = new BasicBlock(cfg);
                cfg.addVertex(basicBlock);
                final SkidBlock mutatedBlock = methodNode.getBlock(basicBlock);

                final HashTransformer transformer = new BitwiseHashTransformer();
                final SkiddedHash hash = transformer.hash(mutatedBlock.getSeed(), methodNode.getLocal());

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
                basicBlock.add(new UnconditionalJumpStmt(
                        exception
                ));
                cfg.addEdge(new UnconditionalJumpEdge<>(
                        basicBlock,
                        exception
                ));

                jump.setTrueSuccessor(basicBlock);
                session.count();
            }

        }
    }

    @Override
    public String getName() {
        return "Condition Mutator";
    }
}
