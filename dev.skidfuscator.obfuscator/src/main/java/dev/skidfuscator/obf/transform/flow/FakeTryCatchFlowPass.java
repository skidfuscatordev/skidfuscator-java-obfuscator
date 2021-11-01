package dev.skidfuscator.obf.transform.flow;


import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.ConditionalJumpEdge;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.ThrowStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.objectweb.asm.Type;

import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WIP
 */
public class FakeTryCatchFlowPass implements FlowPass {
    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

            for (BasicBlock entry : cfg.getEntries()) {
                final Set<FlowEdge<BasicBlock>> edgeList = cfg.getEdges(entry);

                if (edgeList.stream().noneMatch(e -> e instanceof ConditionalJumpEdge))
                    continue;

                final List<ConditionalJumpEdge> conditionalJumpEdges = edgeList
                        .stream()
                        .filter(e -> e instanceof ConditionalJumpEdge)
                        .map(e -> (ConditionalJumpEdge) e)
                        .collect(Collectors.toList());

                for (ConditionalJumpEdge<BasicBlock> conditionalJumpEdge : conditionalJumpEdges) {
                    final ConditionalJumpStmt stmt = conditionalJumpEdge.src()
                            .stream()
                            .filter(e -> e instanceof ConditionalJumpStmt)
                            .map(e -> (ConditionalJumpStmt) e)
                            .filter(e -> e.getOpcode() == conditionalJumpEdge.opcode)
                            .filter(e -> e.getTrueSuccessor().equals(conditionalJumpEdge.dst()))
                            .findFirst()
                            .orElse(null);

                    if (stmt == null)
                        continue;

                    final BasicBlock try_block = new BasicBlock(cfg);
                    final Expr alloc_exception = new AllocObjectExpr(Type.getType(ArrayStoreException.class));
                    final Stmt throw_statement = new ThrowStmt(alloc_exception);
                    try_block.add(throw_statement);
                    try_block.add(new UnconditionalJumpStmt(entry));

                    final UnconditionalJumpEdge<BasicBlock> successor_edge = new UnconditionalJumpEdge<>(try_block, entry);
                    cfg.addVertex(try_block);
                    cfg.addEdge(successor_edge);

                    final BasicBlock handler_block = new BasicBlock(cfg);
                    final ConditionalJumpStmt jump_expr = new ConditionalJumpStmt(stmt.getLeft(), stmt.getRight(), stmt.getTrueSuccessor(), stmt.getComparisonType());
                    handler_block.add(jump_expr);
                    //final ConditionalJumpEdge<BasicBlock> successor
                    //cfg

                    final ExceptionRange<BasicBlock> exception_range = new ExceptionRange<>();
                    exception_range.addVertex(try_block);
                    exception_range.addType(Type.getType(ArrayStoreException.class));
                    exception_range.setHandler(handler_block);
                }
            }

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
