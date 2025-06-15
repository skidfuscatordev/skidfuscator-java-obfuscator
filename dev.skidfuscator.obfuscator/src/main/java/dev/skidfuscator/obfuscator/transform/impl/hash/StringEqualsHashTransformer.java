package dev.skidfuscator.obfuscator.transform.impl.hash;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.transform.AbstractExpressionTransformer;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import sdk.LongHashFunction;

public class StringEqualsHashTransformer extends AbstractExpressionTransformer {
    public StringEqualsHashTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "String Equals Hash");
        requiresSdk();
    }

    @Override
    protected boolean matchesExpression(Expr expr) {
        if (!(expr instanceof InvocationExpr invocationExpr)) {
            return false;
        }

        return invocationExpr.getOwner().equals("java/lang/String")
                && invocationExpr.getName().equals("equals")
                && invocationExpr.getDesc().equals("(Ljava/lang/Object;)Z");
    }

    @Override
    protected boolean transformExpression(Expr expr, ControlFlowGraph cfg) {
        InvocationExpr invocationExpr = (InvocationExpr) expr;
        final Expr[] args = invocationExpr.getArgumentExprs();
        Expr arg0 = args[0];
        Expr arg1 = args[1];

        final boolean isArg0Constant = arg0 instanceof ConstantExpr;
        final boolean isArg1Constant = arg1 instanceof ConstantExpr;

        if (isArg0Constant == isArg1Constant) {
            return false;
        }

        ConstantExpr constantExpr = isArg0Constant ? (ConstantExpr) arg0 : (ConstantExpr) arg1;
        Expr otherExpr = isArg0Constant ? arg1 : arg0;

        constantExpr.setConstant(
                "" + LongHashFunction.xx3().hashChars((String) constantExpr.getConstant())
        );
        otherExpr.getParent().overwrite(otherExpr, new StaticInvocationExpr(
                new Expr[] { otherExpr.copy() },
                "sdk/SDK",
                "hash",
                "(Ljava/lang/String;)Ljava/lang/String;"
        ));

        return true;
    }
}