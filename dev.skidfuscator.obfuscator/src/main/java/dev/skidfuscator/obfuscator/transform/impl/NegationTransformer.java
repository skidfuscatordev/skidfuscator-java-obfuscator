package dev.skidfuscator.obfuscator.transform.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.NegationExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
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
        super(skidfuscator,"Negation", children);
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

        for (BasicBlock vertex : new HashSet<>(cfg.vertices())) {
            for (Stmt stmt : new HashSet<>(vertex)) {
                for (Expr expr : stmt.enumerateOnlyChildren()) {
                    if (!(expr instanceof ConstantExpr))
                        continue;

                    final ConstantExpr constantExpr = (ConstantExpr) expr;

                    if (!TYPES.contains(constantExpr.getType()))
                        continue;

                    final SkidBlock skidBlock = (SkidBlock) vertex;

                    assert constantExpr.getParent() != null;

                    final CodeUnit parent = constantExpr.getParent();

                    final int constant = ((Number) constantExpr.getConstant()).intValue();

                    Expr modified;
                    if (constant < 0) {
                        modified = new NegationExpr(new ConstantExpr(Math.abs(constant)));
                    } else {
                        // I have no clue why this works now, and it didn't previously.
                        modified = new NegationExpr(new NegationExpr(new ConstantExpr(constant)));
                    }
                    parent.overwrite(constantExpr, modified);

                    if (IntegerBlockPredicateRenderer.DEBUG) {
                        final Local local1 = cfg.getLocals().get(cfg.getLocals().getMaxLocals() + 2);
                        vertex.add(
                                vertex.indexOf(stmt) + 1,
                                new SkidCopyVarStmt(
                                        new VarExpr(local1, Type.getType(String.class)),
                                        new ConstantExpr(
                                                "[Constant] "
                                                        + constantExpr.getConstant()
                                                        + " -> "
                                                        + constant
                                                        + " predicate="
                                                        + methodNode.getFlowPredicate().get(skidBlock)
                                        )
                                )
                        );
                    }
                }
            }
        }

        cfg.allExprStream()
                .filter(ConstantExpr.class::isInstance)
                .map(ConstantExpr.class::cast)
                .filter(constantExpr -> TYPES.contains(constantExpr.getType()))
                .collect(Collectors.toList())
                .forEach(unit -> {

                });
    }
}
