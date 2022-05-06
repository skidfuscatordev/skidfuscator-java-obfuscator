package dev.skidfuscator.obfuscator.transform.impl.number;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.TransformResult;
import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NumberTransformer extends AbstractTransformer {
    public NumberTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public NumberTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator, "Number Transformer", children);
    }

    private static final Set<Type> TYPES = new HashSet<>(Arrays.asList(
            Type.INT_TYPE,
            Type.SHORT_TYPE,
            Type.BYTE_TYPE,
            Type.CHAR_TYPE
    ));

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        final Skidfuscator skidfuscator = event.getSkidfuscator();

        if (methodNode.isAbstract() || methodNode.isInit())
            return;

        if (methodNode.node.instructions.size() > 30000)
            return;

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        cfg.allExprStream()
                .filter(ConstantExpr.class::isInstance)
                .map(ConstantExpr.class::cast)
                .filter(constantExpr -> TYPES.contains(constantExpr.getType()))
                .collect(Collectors.toList())
                .forEach(unit -> {
                    final CodeUnit parent = unit.getParent();

                    if (parent == null)
                        return;

                    final SkidBlock skidBlock = (SkidBlock) unit.getBlock();
                    final Expr modified = NumberManager.encrypt(
                            ((Number) unit.getConstant()).intValue(),
                            methodNode.getBlockPredicate(skidBlock),
                            cfg,
                            methodNode.getFlowPredicate().getGetter()
                    );

                    parent.overwrite(unit, modified);
                });

    }

}
