package dev.skidfuscator.obfuscator.skidasm.expr;

import dev.skidfuscator.obfuscator.util.TypeUtil;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;

public class SkidIntegerParseStaticInvocationExpr extends StaticInvocationExpr {
    private final int value;
    public SkidIntegerParseStaticInvocationExpr(final int value) {
        super(
                new Expr[]{new ConstantExpr("" + value, TypeUtil.STRING_TYPE)},
                "java/lang/Integer",
                "parseInt",
                "(Ljava/lang/String;)I"
        );

        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
