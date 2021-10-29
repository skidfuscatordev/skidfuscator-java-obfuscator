package dev.skidfuscator.obf.transform.flow;


import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform.yggdrasil.SkidMethod;
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.ThrowStmt;
import org.objectweb.asm.Type;

import java.util.IllegalFormatCodePointException;

public class FakeJumpFlowPass implements FlowPass {
    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (MethodNode methodNode : method.getMethodNodes()) {
            if (methodNode.isAbstract())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode);

            if (cfg == null)
                continue;

            for (BasicBlock entry : cfg.getEntries()) {
                final Expr var_load = method.getSeed().getPrivateLoader();
                final Expr var_const = new ConstantExpr(method.getSeed().getPrivate());

                final BasicBlock fuckup = new BasicBlock(cfg);
                final Expr alloc_exception = new AllocObjectExpr(Type.getType(IllegalFormatCodePointException.class));
                final Stmt exception_stmt = new ThrowStmt(alloc_exception);
                fuckup.add(exception_stmt);
                cfg.addVertex(fuckup);

                final ConditionalJumpStmt jump_stmt = new ConditionalJumpStmt(var_load, var_const, fuckup, ConditionalJumpStmt.ComparisonType.NE);
                final ConditionalJumpEdge<BasicBlock> jump_edge = new ConditionalJumpEdge<>(entry, fuckup, jump_stmt.getOpcode());
                entry.add(jump_stmt);
                cfg.addEdge(jump_edge);
            }
        }
    }
}
