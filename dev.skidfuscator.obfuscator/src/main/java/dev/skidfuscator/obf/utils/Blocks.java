package dev.skidfuscator.obf.utils;

import lombok.experimental.UtilityClass;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.PopStmt;
import org.mapleir.ir.code.stmt.ThrowStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.io.IOException;

@UtilityClass
public class Blocks {
    // A list of random exceptions
    private final Class<?>[] exceptionClasses = new Class[] {
            IllegalStateException.class,
            IllegalArgumentException.class,
            IllegalAccessException.class,
            IOException.class,
            RuntimeException.class,
            ExceptionInInitializerError.class
    };

    public BasicBlock exception(final ControlFlowGraph cfg) {
        // Temporary fix for this
        final Type exception = Type.getType(exceptionClasses[RandomUtil.nextInt(exceptionClasses.length - 1)]);

        final BasicBlock fuckup = new BasicBlock(cfg);
        final Expr alloc_exception = new AllocObjectExpr(exception);
        final Local local = cfg.getLocals().get(cfg.getEntries().size() + 2, true);

        final VarExpr dup_save = new VarExpr(local, exception);
        final Stmt dup_stmt = new CopyVarStmt(dup_save, alloc_exception, true);
        fuckup.add(dup_stmt);

        final VarExpr fuck = new VarExpr(local, exception);
        final Expr init_alloc = new VirtualInvocationExpr(
                InvocationExpr.CallType.SPECIAL,
                new Expr[]{fuck},
                exception.getClassName().replace(".", "/"),
                "<init>",
                "()V"
        );
        final PopStmt popStmt = new PopStmt(init_alloc);
        fuckup.add(popStmt);

        final VarExpr returnFuck = new VarExpr(local, exception);
        final Stmt exception_stmt = new ThrowStmt(returnFuck);
        fuckup.add(exception_stmt);

        cfg.addVertex(fuckup);

        return fuckup;
    }
}
