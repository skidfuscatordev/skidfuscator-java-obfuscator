package dev.skidfuscator.obfuscator.transform.impl.number;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidConstantExpr;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NumberTransformer extends AbstractTransformer {
    public NumberTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public NumberTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator, "Number Encryption", children);
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

        if (methodNode.node.instructions.size() > 10000)
            return;

        final SkidControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        for (BasicBlock vertex : new HashSet<>(cfg.vertices())) {
            if (vertex.isFlagSet(SkidBlock.FLAG_NO_OPAQUE))
                continue;

            for (Stmt stmt : new HashSet<>(vertex)) {
                for (Expr expr : stmt.enumerateOnlyChildren()) {
                    if (!(expr instanceof ConstantExpr) || expr instanceof SkidConstantExpr)
                        continue;

                    final ConstantExpr constantExpr = (ConstantExpr) expr;

                    if (!TYPES.contains(constantExpr.getType()))
                        continue;

                    final SkidBlock skidBlock = (SkidBlock) vertex;

                    assert constantExpr.getParent() != null;

                    final CodeUnit parent = constantExpr.getParent();
                    final BlockOpaquePredicate flowPredicate = methodNode.getFlowPredicate();
                    final int predicate = flowPredicate.get(skidBlock);

                    final int xored = ((Number) constantExpr.getConstant()).intValue()
                            ^ predicate;
                    final Expr modified = new XorNumberTransformer().getNumber(
                            ((Number) constantExpr.getConstant()).intValue(),
                            predicate,
                            skidBlock,
                            flowPredicate.getGetter()
                    );

                    //constantExpr.setConstant(xored);
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
                                                        + xored
                                                        + " predicate="
                                                        + predicate
                                        )
                                )
                        );
                    }
                }
            }
        }
    }
}
