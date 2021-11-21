package dev.skidfuscator.obf.transform.impl.flow;


import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.maple.FakeConditionalJumpStmt;
import dev.skidfuscator.obf.number.hash.SkiddedHash;
import dev.skidfuscator.obf.number.hash.impl.BitwiseHashTransformer;
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
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.*;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WIP
 */
public class FakeTryCatchFlowPass implements FlowPass {
    private static final Type TYPE = Type.getType(ArrayStoreException.class);

    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

            final Local local = cfg.getLocals().get(cfg.getEntries().size() + 2, true);
            for (BasicBlock entry : new HashSet<>(cfg.vertices())) {
                for (Stmt _stmt : new HashSet<>(entry)) {
                    if (!(_stmt instanceof ConditionalJumpStmt)) {
                        continue;
                    }

                    final ConditionalJumpEdge<BasicBlock> edge = cfg
                            .getEdges(entry)
                            .stream()
                            .filter(e -> e instanceof ConditionalJumpEdge)
                            .map(e -> (ConditionalJumpEdge) e)
                            .filter(e -> e.dst() == ((ConditionalJumpStmt) _stmt).getTrueSuccessor())
                            .findFirst()
                            .orElse(null);

                    if (edge == null)
                        continue;

                    final ConditionalJumpStmt stmt = (ConditionalJumpStmt) _stmt;

                    final Expr right = stmt.getRight();
                    final Expr left = stmt.getLeft();

                    if (right == null || left == null)
                        continue;

                    right.unlink();
                    left.unlink();

                    final BasicBlock try_block = new BasicBlock(cfg);
                    final Expr alloc_exception = new AllocObjectExpr(TYPE);
                    final CopyVarStmt copy_stmt = new CopyVarStmt(new VarExpr(local, TYPE), alloc_exception);
                    final Expr init_expr = new VirtualInvocationExpr(
                            InvocationExpr.CallType.SPECIAL,
                            new Expr[]{new VarExpr(local, TYPE)},
                            TYPE.getClassName().replace(".", "/"),
                            "<init>",
                            "()V"
                    );
                    final PopStmt pop_init = new PopStmt(init_expr);
                    final Stmt throw_statement = new ThrowStmt(new VarExpr(local, TYPE));
                    try_block.add(copy_stmt);
                    try_block.add(pop_init);
                    try_block.add(throw_statement);
                    try_block.add(new UnconditionalJumpStmt(entry));

                    final UnconditionalJumpEdge<BasicBlock> successor_edge = new UnconditionalJumpEdge<>(try_block, entry);
                    cfg.addVertex(try_block);
                    cfg.addEdge(successor_edge);

                    final BasicBlock handler_block = new BasicBlock(cfg);
                    final ConditionalJumpStmt jump_expr = new ConditionalJumpStmt(left, right, stmt.getTrueSuccessor(), stmt.getComparisonType());
                    handler_block.add(jump_expr);
                    cfg.addVertex(handler_block);
                    cfg.addEdge(new ConditionalJumpEdge<>(handler_block, stmt.getTrueSuccessor(), jump_expr.getOpcode()));

                    // Todo add hashing to amplify difficulty and remove key exposure
                    // Todo make this a better system
                    final int seed = methodNode.getBlock(entry).getSeed();

                    // Create hash
                    final SkiddedHash hash = new BitwiseHashTransformer().hash(seed, methodNode.getLocal());
                    final ConstantExpr var_const = new ConstantExpr(hash.getHash());

                    entry.set(entry.indexOf(stmt), new ConditionalJumpStmt(
                            hash.getExpr(),
                            var_const,
                            try_block,
                            ConditionalJumpStmt.ComparisonType.EQ
                    ));
                    cfg.removeEdge(edge);
                    cfg.addEdge(new ConditionalJumpEdge<>(entry, try_block, Opcodes.IF_ICMPEQ));
                    //final ConditionalJumpEdge<BasicBlock> successor
                    //cfg

                    final ExceptionRange<BasicBlock> exception_range = new ExceptionRange<>();
                    exception_range.addVertex(try_block);
                    exception_range.addType(Type.getType(ArrayStoreException.class));
                    exception_range.setHandler(handler_block);

                    session.count();
                }
            }

            /*for (BasicBlock entry : cfg.getEntries()) {
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
            }*/
        }
    }

    @Override
    public String getName() {
        return "Fake Try Catch";
    }
}
