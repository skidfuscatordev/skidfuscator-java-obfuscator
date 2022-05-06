package dev.skidfuscator.obfuscator.transform.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.NegationExpr;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This transformer concept seen in scuti's String Encryption Transformer.
 * {@see https://github.com/netindev/scuti/blob/467b856b7ea46009608ccdf4db69b4b43e640fa6/scuti-core/src/main/java/tk/netindev/scuti/core/transform/obfuscation/StringEncryptionTransformer.java#L172}
 *
 * This simply alters the instructions of a method to make reversing a bit more annoying.
 *
 * BEFORE ->
 * LDC INTEGER -5120
 *
 * OR ->
 * LDC INTEGER 5120
 *
 * AFTER ->
 * INEG
 * LDC INTEGER 5120
 *
 * OR ->
 * INEG
 * INEG
 * LDC INTEGER 5120
 *
 * @author ryan (vaz@mdma.dev)
 */
public class NegationTransformer extends AbstractTransformer {
    private static final Set<Type> TYPES = new HashSet<>(Arrays.asList(
            Type.INT_TYPE,
            Type.SHORT_TYPE,
            Type.BYTE_TYPE,
            Type.CHAR_TYPE
    ));

    public NegationTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public NegationTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"Negation Transformer", children);
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        if (methodNode.isAbstract() || methodNode.isInit())
            return;

        if (methodNode.node.instructions.size() > 10000)
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

                    final int constant = ((Number) unit.getConstant()).intValue();

                    Expr modified;

                    if (constant < 0) {
                        modified = new NegationExpr(new ConstantExpr(Math.abs(constant)));
                    } else {
                        // I have no clue why this works now, and it didn't previously.
                        modified = new NegationExpr(new NegationExpr(new ConstantExpr(constant)));
                    }

                    parent.overwrite(unit, modified);
                });
    }
}
