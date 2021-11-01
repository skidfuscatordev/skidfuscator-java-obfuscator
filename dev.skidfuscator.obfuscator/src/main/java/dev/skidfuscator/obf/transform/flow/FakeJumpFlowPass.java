package dev.skidfuscator.obf.transform.flow;


import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.maple.FakeConditionalJumpStmt;
import dev.skidfuscator.obf.transform.flow.gen3.SkidGraph;
import dev.skidfuscator.obf.transform.yggdrasil.SkidMethod;
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.code.stmt.ThrowStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.HashSet;


public class FakeJumpFlowPass implements FlowPass {
    private static final Type EXCEPTION = Type.getType(IllegalStateException.class);

    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

            for (BasicBlock entry : new HashSet<>(cfg.vertices())) {
                if (entry.size() == 0)
                    continue;

                final Expr var_load = new VarExpr(methodNode.getLocal(), Type.INT_TYPE);
                final ConstantExpr var_const = new ConstantExpr(methodNode.getBlock(entry).getSeed());

                final BasicBlock fuckup = new BasicBlock(cfg);
                final Expr alloc_exception = new AllocObjectExpr(EXCEPTION);
                final Local local = cfg.getLocals().get(cfg.getEntries().size() + 2, true);

                final VarExpr dup_save = new VarExpr(local, EXCEPTION);
                final Stmt dup_stmt = new CopyVarStmt(dup_save, alloc_exception, true);
                fuckup.add(dup_stmt);

                final VarExpr fuck = new VarExpr(local, EXCEPTION);
                final Expr init_alloc = new VirtualInvocationExpr(
                        InvocationExpr.CallType.SPECIAL,
                        new Expr[]{fuck},
                        EXCEPTION.getClassName().replace(".", "/"),
                        "<init>",
                        "()V"
                );
                final PopStmt popStmt = new PopStmt(init_alloc);
                fuckup.add(popStmt);

                final VarExpr returnFuck = new VarExpr(local, EXCEPTION);
                final Stmt exception_stmt = new ThrowStmt(returnFuck);
                fuckup.add(exception_stmt);

                cfg.addVertex(fuckup);

                final FakeConditionalJumpStmt jump_stmt = new FakeConditionalJumpStmt(var_load, var_const, fuckup, ConditionalJumpStmt.ComparisonType.NE);
                final ConditionalJumpEdge<BasicBlock> jump_edge = new ConditionalJumpEdge<>(entry, fuckup, jump_stmt.getOpcode());
                entry.add(jump_stmt);
                cfg.addEdge(jump_edge);

                /*final Local local1 = entry.cfg.getLocals().get(entry.cfg.getLocals().getMaxLocals() + 2);
                entry.add(new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                        new ConstantExpr(entry.getDisplayName() +" : var expect: " + var_const.getConstant())));
                */
            }
        }
    }
}
