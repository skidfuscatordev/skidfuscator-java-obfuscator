package dev.skidfuscator.obfuscator.transform.impl.hash;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.AbstractExpressionTransformer;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import sdk.LongHashFunction;

import static dev.skidfuscator.obfuscator.manifold.InvokeExt.invoke;

public class StringEqualsIgnoreCaseHashTransformer extends AbstractExpressionTransformer {
    public StringEqualsIgnoreCaseHashTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "String EqIgCase Hash");
        requiresSdk();
    }

    @Override
    protected boolean matchesExpression(Expr expr) {
        if (!(expr instanceof InvocationExpr invocationExpr)) {
            return false;
        }

        return invocationExpr.getOwner().equals("java/lang/String")
                && invocationExpr.getName().equals("equalsIgnoreCase")
                && invocationExpr.getDesc().equals("(Ljava/lang/String;)Z");
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
                "" + LongHashFunction.xx3().hashChars(((String) constantExpr.getConstant()).toLowerCase())
        );

        otherExpr.getParent().overwrite(otherExpr, new StaticInvocationExpr(
                new Expr[] { new VirtualInvocationExpr(
                        InvocationExpr.CallType.VIRTUAL,
                        new Expr[]{otherExpr.copy()},
                        "java/lang/String",
                        "toLowerCase",
                        "()Ljava/lang/String;"
                )},
                "sdk/SDK",
                "hash",
                "(Ljava/lang/String;)Ljava/lang/String;"
        ));

        return true;
    }
}