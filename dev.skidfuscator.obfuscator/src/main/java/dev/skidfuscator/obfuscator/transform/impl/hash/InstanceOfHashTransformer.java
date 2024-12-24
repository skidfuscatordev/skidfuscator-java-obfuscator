package dev.skidfuscator.obfuscator.transform.impl.hash;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.transform.AbstractExpressionTransformer;
import dev.skidfuscator.obfuscator.transform.impl.number.NumberTransformer;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.InstanceofExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.objectweb.asm.Type;
import sdk.LongHashFunction;

public class InstanceOfHashTransformer extends AbstractExpressionTransformer {
    public InstanceOfHashTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Type Check");
        requiresSdk();
    }

    private final int seed = RandomUtil.nextInt();

    @Override
    protected boolean matchesExpression(Expr expr) {
        final boolean valid = expr instanceof InstanceofExpr;

        if (valid) {
            System.out.println("Checking instanceof expression: " + expr);
        }

        return valid;
    }

    @Override
    protected boolean transformExpression(Expr expr, ControlFlowGraph cfg) {
        final SkidMethodNode methodNode = (SkidMethodNode) cfg.getMethodNode();
        final int blockPredicate = methodNode.getBlockPredicate((SkidBlock) expr.getBlock());
        final BlockOpaquePredicate predicate = methodNode.getFlowPredicate();

        InstanceofExpr instanceofExpr = (InstanceofExpr) expr;
        Type checkType = instanceofExpr.getCheckType();
        Expr object = instanceofExpr.getExpression();

        System.out.println("Checking instanceof expression: " + expr);

        // Skip primitive types and arrays
        if (checkType.getSort() != Type.OBJECT || checkType.getDescriptor().startsWith("[")) {
            return false;
        }

        // Create a new static invocation to our custom type check method
        StaticInvocationExpr replacement = new StaticInvocationExpr(
                new Expr[] { 
                    object.copy(),
                    // Pass the type hash as a string constant
                    new ConstantExpr("" + LongHashFunction
                            .xx3(seed)
                            .hashChars(checkType.getInternalName())
                    ),
                        NumberManager.encrypt(seed, blockPredicate, expr.getBlock(), predicate.getGetter())
                },
                "sdk/SDK",
                "checkType",
                "(Ljava/lang/Object;Ljava/lang/String;I)Z"
        );

        System.out.println("Transformed instanceof expression: " + expr + " to " + replacement);
        expr.getParent().overwrite(expr, replacement);
        return true;
    }
}