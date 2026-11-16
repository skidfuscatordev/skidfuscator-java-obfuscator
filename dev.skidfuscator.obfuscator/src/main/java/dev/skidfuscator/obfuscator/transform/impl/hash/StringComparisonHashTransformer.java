package dev.skidfuscator.obfuscator.transform.impl.hash;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.transform.AbstractExpressionTransformer;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import sdk.LongHashFunction;
import sdk.SDK;

/** Hide one non-null literal while retaining the target JVM's exact String comparison. */
abstract class StringComparisonHashTransformer extends AbstractExpressionTransformer {
    private final boolean ignoreCase;

    StringComparisonHashTransformer(Skidfuscator skidfuscator, String name, boolean ignoreCase) {
        super(skidfuscator, name);
        this.ignoreCase = ignoreCase;
        requiresSdk();
    }

    @Override
    protected boolean matchesExpression(Expr expr) {
        if (!(expr instanceof VirtualInvocationExpr invocation)) return false;
        return invocation.getCallType() == InvocationExpr.CallType.VIRTUAL
                && invocation.getOwner().equals("java/lang/String")
                && invocation.getName().equals(ignoreCase ? "equalsIgnoreCase" : "equals")
                && invocation.getDesc().equals(ignoreCase ? "(Ljava/lang/String;)Z" : "(Ljava/lang/Object;)Z")
                && invocation.getArgumentExprs().length == 2;
    }

    @Override
    protected boolean transformExpression(Expr expr, ControlFlowGraph cfg) {
        if (!matchesExpression(expr) || expr.getParent() == null) return false;
        Expr[] arguments = ((InvocationExpr) expr).getArgumentExprs();
        boolean receiverIsConstant = arguments[0] instanceof ConstantExpr;
        boolean argumentIsConstant = arguments[1] instanceof ConstantExpr;
        if (receiverIsConstant == argumentIsConstant) return false;
        int literalIndex = receiverIsConstant ? 0 : 1;
        Object constant = ((ConstantExpr) arguments[literalIndex]).getConstant();
        // A null literal is not a String value. Preserve the original invocation,
        // including false for a null argument and NPE for a null receiver.
        if (!(constant instanceof String value)) return false;
        long key = LongHashFunction.xx3().hashChars(value);
        String encoded = SDK.maskComparisonLiteral(value, key);
        if (modifiedUtf8Length(encoded) > 65535) return false;

        Expr receiver = receiverIsConstant ? new ConstantExpr(encoded) : arguments[0].copy();
        Expr argument = argumentIsConstant ? new ConstantExpr(encoded) : arguments[1].copy();
        String helper = (ignoreCase ? "equalsIgnoreCase" : "equals")
                + (receiverIsConstant ? "ConstantReceiver" : "ConstantArgument");
        String descriptor = !ignoreCase && receiverIsConstant
                ? "(Ljava/lang/String;Ljava/lang/Object;J)Z" : "(Ljava/lang/String;Ljava/lang/String;J)Z";
        expr.getParent().overwrite(expr, new StaticInvocationExpr(
                new Expr[]{receiver, argument, new ConstantExpr(key)}, "sdk/SDK", helper, descriptor));
        return true;
    }

    private static int modifiedUtf8Length(String value) {
        int length = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            length += character >= 1 && character <= 127 ? 1 : character <= 2047 ? 2 : 3;
            if (length > 65535) break;
        }
        return length;
    }
}
