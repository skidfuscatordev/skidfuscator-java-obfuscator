package dev.skidfuscator.obfuscator.transform.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidSwitchStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class SwitchTransformer extends AbstractTransformer {
    public SwitchTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public SwitchTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"Flow Switch", children);
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

        cfg.vertices()
                .stream()
                .flatMap(Collection::stream)
                .filter(SkidSwitchStmt.class::isInstance)
                .map(SwitchStmt.class::cast)
                .collect(Collectors.toList())
                .forEach(unit -> {
                    final BlockOpaquePredicate blockOpaquePredicate = methodNode.getFlowPredicate();
                    final int opaquePredicate = blockOpaquePredicate.get((SkidBlock) unit.getBlock());

                    final LinkedHashMap<Integer, BasicBlock> targets = unit.getTargets();
                    final LinkedHashMap<Integer, BasicBlock> targetsUpdated = new LinkedHashMap<>();

                    final HashTransformer hasher = NumberManager.randomHasher(skidfuscator);

                    targets.forEach((var, bb) -> {
                        final int hash = hasher.hash(var ^ opaquePredicate);
                        targetsUpdated.put(hash, bb);
                    });

                    unit.setTargets(targetsUpdated);

                    final Expr expr = unit.getExpression();
                    expr.unlink();

                    final Local local = cfg.getLocals().get(
                            cfg.getLocals().getMaxLocals() + 2,
                            true
                    );
                    local.setType(Type.INT_TYPE);
                    final CopyVarStmt copyVarStmt = new CopyVarStmt(
                            new VarExpr(local, Type.INT_TYPE),
                            new FakeArithmeticExpr(
                                    expr,
                                    blockOpaquePredicate.getGetter().get(unit.getBlock()),
                                    ArithmeticExpr.Operator.XOR
                            )
                    );
                    unit.getBlock().add(unit.getBlock().indexOf(unit), copyVarStmt);

                    unit.setExpression(hasher.hash(unit.getBlock(), new PredicateFlowGetter() {
                        @Override
                        public Expr get(final BasicBlock vertex) {
                            return new VarExpr(local, Type.INT_TYPE);
                        }
                    }));
                });
    }
}
