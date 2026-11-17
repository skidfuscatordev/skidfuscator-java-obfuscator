package dev.skidfuscator.obfuscator.transform.impl.hash;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.config.DefaultTransformerConfig;
import dev.skidfuscator.obfuscator.transform.AbstractExpressionTransformer;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.InstanceofExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;

/** Retain the JVM's class/loader identity rather than comparing hashes of names. */
public class InstanceOfHashTransformer extends AbstractExpressionTransformer {
    public InstanceOfHashTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Type Check");
        requiresSdk();
    }

    @Override protected DefaultTransformerConfig createConfig() {
        String path = skidfuscator.getTsConfig().hasPath("instanceOfHash") ? "instanceOfHash" : "typeCheck";
        return new DefaultTransformerConfig(skidfuscator.getTsConfig(), path);
    }

    @Override protected boolean matchesExpression(Expr expr) { return expr instanceof InstanceofExpr; }

    @Override protected boolean transformExpression(Expr expr, ControlFlowGraph cfg) {
        if (!(expr instanceof InstanceofExpr check) || expr.getParent() == null) return false;
        expr.getParent().overwrite(expr, new StaticInvocationExpr(
                new Expr[]{check.getExpression().copy(), new ConstantExpr(check.getCheckType())},
                Skidfuscator.SDK_OWNER, "checkType", "(Ljava/lang/Object;Ljava/lang/Class;)Z"));
        return true;
    }
}
