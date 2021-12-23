package dev.skidfuscator.obf.utils;

import lombok.experimental.UtilityClass;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.AllocObjectExpr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
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

    public BasicBlock exception(final ControlFlowGraph cfg) {
        return exception(cfg, null);
    }

    public BasicBlock exception(final ControlFlowGraph cfg, final String notice) {
        // Temporary fix for this
        final Type exception = Type.getType(exceptionClasses[RandomUtil.nextInt(exceptionClasses.length - 1)]);

        final BasicBlock fuckup = new BasicBlock(cfg);
        final Expr alloc_exception = new AllocObjectExpr(exception);
        final Local local = cfg.getLocals().get(cfg.getEntries().size() + 2, true);

        final VarExpr dup_save = new VarExpr(local, exception);
        final Stmt dup_stmt = new CopyVarStmt(dup_save, alloc_exception, true);
        fuckup.add(dup_stmt);

        final VarExpr fuck = new VarExpr(local, exception);
        final Expr init_alloc;

        if (notice == null) {
            init_alloc = new VirtualInvocationExpr(
                    InvocationExpr.CallType.SPECIAL,
                    new Expr[]{fuck},
                    exception.getClassName().replace(".", "/"),
                    "<init>",
                    "()V"
            );
        } else {
            init_alloc = new VirtualInvocationExpr(
                    InvocationExpr.CallType.SPECIAL,
                    new Expr[]{fuck, bruteforceInterpolation(new ConstantExpr(notice))},
                    exception.getClassName().replace(".", "/"),
                    "<init>",
                    "(Ljava/lang/String;)V"
            );
        }

        final PopStmt popStmt = new PopStmt(init_alloc);
        fuckup.add(popStmt);

        final VarExpr returnFuck = new VarExpr(local, exception);
        final Stmt exception_stmt = new ThrowStmt(returnFuck);
        fuckup.add(exception_stmt);

        cfg.addVertex(fuckup);

        return fuckup;
    }

    public Expr bruteforceInterpolation(final Expr string) {
        final int random = RandomUtil.nextInt(35);

        Expr start = randomDouble();

        for (int i = 0; i < random; i++) {
            switch (RandomUtil.nextInt(5)) {
                case 0: {
                    start = new ArithmeticExpr(start, randomDouble(), ArithmeticExpr.Operator.ADD);
                    break;
                }
                case 1: {
                    start = new ArithmeticExpr(start, randomDouble(), ArithmeticExpr.Operator.SUB);
                    break;
                }
                case 2: {
                    start = new ArithmeticExpr(start, randomDouble(), ArithmeticExpr.Operator.MUL);
                    break;
                }
                case 3: {
                    start = new ArithmeticExpr(start, randomDouble(), ArithmeticExpr.Operator.DIV);
                    break;
                }
                case 4: {
                    start = new ArithmeticExpr(start, randomDouble(), ArithmeticExpr.Operator.REM);
                    break;
                }
            }
        }

        final Expr doubleToString = new StaticInvocationExpr(
                new Expr[]{start},
                "java/lang/Double",
                "toString",
                "(D)Ljava/lang/String;"
        );


        final Expr stringBuilder = new InitialisedObjectExpr("java/lang/StringBuilder", "()V", new Expr[0]);
        final Expr append = new VirtualInvocationExpr(
                InvocationExpr.CallType.VIRTUAL, new Expr[]{stringBuilder, string},
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;"
        );

        final Expr append2 = new VirtualInvocationExpr(
                InvocationExpr.CallType.VIRTUAL, new Expr[]{append, doubleToString},
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;"
        );

        final Expr toString = new VirtualInvocationExpr(
                InvocationExpr.CallType.VIRTUAL, new Expr[]{append2},
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;"
        );

        return toString;
    }

    public Expr randomDouble() {
        return new ConstantExpr(RandomUtil.nextDouble(Integer.MAX_VALUE), Type.DOUBLE_TYPE);
    }
}
