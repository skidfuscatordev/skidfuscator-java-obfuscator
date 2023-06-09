package dev.skidfuscator.obfuscator.util.cfg;

import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import lombok.experimental.UtilityClass;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
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
    private final Class<?>[] exceptionClasses = new Class<?>[] {
            IllegalAccessException.class,
            IOException.class,
            RuntimeException.class,
            ArrayStoreException.class
    };

    public SkidBlock exception(final ControlFlowGraph cfg) {
        return exception(cfg, null);
    }

    public SkidBlock exception(final ControlFlowGraph cfg, final String notice) {
        // Temporary fix for this
        final Type exception = Type.getType(exceptionClasses[RandomUtil.nextInt(exceptionClasses.length - 1)]);

        final SkidBlock fuckup = new SkidBlock(cfg);
        final Expr alloc_exception = new InitialisedObjectExpr(
                exception.getClassName().replace(".", "/"),
                notice == null ? "()V" : "(Ljava/lang/String;)V",
                notice == null ? new Expr[0] : new Expr[]{new ConstantExpr(notice)}
        );

        final Stmt exception_stmt = new ThrowStmt(alloc_exception);
        fuckup.add(exception_stmt);

        cfg.addVertex(fuckup);

        return fuckup;
    }
}
