package dev.skidfuscator.obfuscator.protection;

import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import org.mapleir.ir.code.expr.ConstantExpr;

public class WebhookProtectionProvider implements ProtectionProvider {

    @Listen
    void handle(final InitMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        methodNode.getCfg().allExprStream()
                .filter(ConstantExpr.class::isInstance)
                .map(ConstantExpr.class::cast)
                .filter(e -> e.getType().equals(TypeUtil.STRING_TYPE))
                .forEach(e -> {

                });
    }

    @Override
    public boolean shouldWarn() {
        return false;
    }

    @Override
    public String getWarning() {
        return null;
    }
}
