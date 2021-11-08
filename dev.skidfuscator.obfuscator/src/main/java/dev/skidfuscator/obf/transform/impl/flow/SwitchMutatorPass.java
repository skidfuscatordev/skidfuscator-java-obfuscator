package dev.skidfuscator.obf.transform.impl.flow;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.maple.FakeArithmeticExpr;
import dev.skidfuscator.obf.number.NumberManager;
import dev.skidfuscator.obf.skidasm.SkidBlock;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SwitchMutatorPass implements FlowPass {
    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract() || methodNode.isInit())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

            for (Stmt stmt : cfg.stmts()) {
                if (!(stmt instanceof SwitchStmt)) {
                    continue;
                }

                final SwitchStmt switchStmt = (SwitchStmt) stmt;

                final SkidBlock skidBlock = methodNode.getBlock(switchStmt.getBlock());
                final Expr switchExpr = switchStmt.getExpression();
                switchExpr.unlink();
                final Expr expr = new FakeArithmeticExpr(
                        switchExpr,
                        new VarExpr(methodNode.getLocal(), Type.INT_TYPE),
                        ArithmeticExpr.Operator.XOR
                );

                switchStmt.setExpression(expr);

                final Set<Map.Entry<Integer, BasicBlock>> entrySet = new HashSet<>(switchStmt.getTargets().entrySet());
                switchStmt.getTargets().clear();

                for (Map.Entry<Integer, BasicBlock> entry : entrySet) {
                    switchStmt.getTargets().put(entry.getKey() ^ skidBlock.getSeed(), entry.getValue());
                }

                session.count();
            }
        }
    }

    @Override
    public String getName() {
        return "Switch Mutator";
    }
}
