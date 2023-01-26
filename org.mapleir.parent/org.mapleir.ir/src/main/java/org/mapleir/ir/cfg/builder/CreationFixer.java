package org.mapleir.ir.cfg.builder;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.utils.Parameter;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CreationFixer extends ControlFlowGraphBuilder.BuilderPass {
    public CreationFixer(ControlFlowGraphBuilder builder) {
        super(builder);
    }

    private final AtomicInteger fixed = new AtomicInteger();

    @Override
    public void run() {
        for (BasicBlock vertex : builder.graph.vertices()) {
            CopyVarStmt currentStmt = null;
            AllocObjectExpr currentAllocation = null;
            Set<Local> currentLocal = null;

            for (Stmt stmt : new HashSet<>(vertex)) {
                if (stmt instanceof CopyVarStmt) {
                    final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;
                    if (copyVarStmt.getExpression() instanceof AllocObjectExpr) {
                        currentStmt = copyVarStmt;
                        currentAllocation = (AllocObjectExpr) copyVarStmt.getExpression();
                        currentLocal = new HashSet<>(Collections.singletonList(
                                copyVarStmt.getVariable().getLocal()));

                        System.out.println("Found allocation for " + currentAllocation.getType() + " of " + copyVarStmt);
                    } else if (copyVarStmt.getExpression() instanceof VarExpr && ((VarExpr) copyVarStmt.getExpression()).getLocal().equals(currentLocal)) {
                        System.out.println("Found synthetic from " + copyVarStmt);
                        currentLocal.add(copyVarStmt.getVariable().getLocal());
                    }
                } else if (stmt instanceof PopStmt) {
                    final PopStmt popStmt = (PopStmt) stmt;

                    if (currentAllocation != null &&
                            popStmt.getExpression() instanceof VirtualInvocationExpr) {
                        final VirtualInvocationExpr invoke = (VirtualInvocationExpr) popStmt.getExpression();

                        if (invoke.getName().equals("<init>")) {
                            System.out.println("Found virtual invoke " + invoke + " of args " + Arrays.toString(invoke.getArgumentExprs()));
                        }
                        if (invoke.getArgumentExprs()[0] instanceof VarExpr) {
                            final VarExpr varExpr = (VarExpr) invoke.getArgumentExprs()[0];

                            System.out.println("Matching " + currentLocal + " with " + varExpr.getLocal());
                            if (currentLocal.contains(varExpr.getLocal())) {
                                System.out.println("Found initializer " + invoke);
                                final Expr[] args = new Expr[invoke.getArgumentExprs().length - 1];
                                for (int i = 1; i < invoke.getArgumentExprs().length; i++) {
                                    invoke.getArgumentExprs()[i].unlink();
                                    args[i - 1] = invoke.getArgumentExprs()[i];

                                    assert args[i - 1] != null : "Null argument at index " + i + "?: "
                                            + Arrays.asList(invoke.getArgumentExprs());
                                }


                                invoke.unlink();
                                currentStmt.setExpression(
                                        new InitialisedObjectExpr(
                                                currentAllocation
                                                        .getType()
                                                        .getClassName()
                                                        .replace(".", "/"),
                                                invoke.getDesc(),
                                                args
                                        )
                                );
                                System.out.println("---------------");
                                currentAllocation = null;
                                currentLocal = null;
                                currentStmt = null;
                                fixed.incrementAndGet();
                            }
                        }
                    }
                } else {
                    for (Expr expr : stmt.enumerateOnlyChildren()) {
                        if (expr instanceof VarExpr) {
                            final VarExpr varExpr = (VarExpr) expr;
                            if (currentLocal != null && currentLocal.contains(varExpr.getLocal())) {
                                System.out.println("(!) --> " + varExpr);
                            }
                        } else if (expr instanceof VirtualInvocationExpr) {
                            final VirtualInvocationExpr virtualInvocationExpr = (VirtualInvocationExpr) expr;

                            if (virtualInvocationExpr.getName().equals("<init>")) {
                                System.out.println("[!] --> " + virtualInvocationExpr.getRootParent());
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Fixed " + fixed.get() + " initialisations");
    }
}
