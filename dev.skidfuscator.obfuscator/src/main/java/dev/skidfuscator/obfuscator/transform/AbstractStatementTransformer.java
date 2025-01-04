package dev.skidfuscator.obfuscator.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventPriority;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;

import java.util.HashSet;

public interface AbstractStatementTransformer<T extends Stmt> extends Transformer {
    @Listen(EventPriority.LOWEST)
    default void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        if (shouldSkipMethod(methodNode)) {
            this.skip();
            return;
        }

        final ControlFlowGraph cfg = methodNode.getCfg();
        if (cfg == null) {
            this.fail();
            return;
        }

        for (BasicBlock vertex : new HashSet<>(cfg.vertices())) {
            if (vertex.isFlagSet(SkidBlock.FLAG_NO_OPAQUE))
                continue;

            if (methodNode.isClinit()) {
                continue;
            }

            for (Stmt stmt : new HashSet<>(vertex)) {
                if (matchStatement(stmt)) {
                    if (transformStatement((T) stmt, cfg)) {
                        this.success();
                    }
                }
            }
        }
    }

    default boolean shouldSkipMethod(SkidMethodNode methodNode) {
        return methodNode.isAbstract() 
                || methodNode.isInit() 
                || methodNode.node.instructions.size() > 10000;
    }

    boolean matchStatement(Stmt stmt);
    boolean transformStatement(T stmt, ControlFlowGraph cfg);
}