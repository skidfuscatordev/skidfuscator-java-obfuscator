package dev.skidfuscator.obf.transform.impl.flow;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.maple.FakeConditionalJumpStmt;
import dev.skidfuscator.obf.number.NumberManager;
import dev.skidfuscator.obf.number.hash.HashTransformer;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obf.skidasm.SkidBlock;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import dev.skidfuscator.obf.utils.Blocks;
import org.mapleir.X;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ComparisonExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.objectweb.asm.Type;

import java.util.HashSet;

public class ConditionV2MutatorPass implements FlowPass {
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

                for (Stmt stmt : new HashSet<>(parent)) {
                    if (!(stmt instanceof ConditionalJumpStmt)) {
                        continue;
                    }

                    final ConditionalJumpStmt jumpStmt = (ConditionalJumpStmt) stmt;

                    if (!jumpStmt.getComparisonType().equals(ConditionalJumpStmt.ComparisonType.EQ))
                        continue;

                    final SkidBlock mutatedBlock = methodNode.getBlock(parent);

                    final Expr right = jumpStmt.getRight();
                    final Expr left = jumpStmt.getLeft();

                    if (right == null || left == null)
                        continue;

                    right.unlink();
                    left.unlink();

                    final ComparisonExpr real_expr = new ComparisonExpr(left, right, ComparisonExpr.ValueComparisonType.CMP);

                    final SkiddedHash hash = NumberManager.hash(mutatedBlock.getSeed(), methodNode.getLocal());
                    final ComparisonExpr fake_expr = new ComparisonExpr(hash.getExpr(), new ConstantExpr(hash.getHash()), ComparisonExpr.ValueComparisonType.CMP);

                    final FakeConditionalJumpStmt conditionalJumpStmt = new FakeConditionalJumpStmt(real_expr, fake_expr, jumpStmt.getTrueSuccessor(), ConditionalJumpStmt.ComparisonType.EQ);
                    parent.set(parent.indexOf(stmt), conditionalJumpStmt);
                }
            }

        }
    }

    @Override
    public String getName() {
        return "Condition Mutator";
    }
}
