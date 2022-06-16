package org.mapleir.ir.cfg.builder;

import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class LocalFixerPass extends ControlFlowGraphBuilder.BuilderPass {
    public LocalFixerPass(ControlFlowGraphBuilder builder) {
        super(builder);
    }

    @Override
    public void run() {
        Map<Integer, Type> types = new HashMap<>();
        int index = 0;

        if (!builder.method.isStatic()) {
            types.put(index, Type.getType("L" + builder.method.getOwner() + ";"));
            index++;
        }

        for (Type argumentType : Type.getArgumentTypes(builder.method.getDesc())) {
            types.put(index, argumentType);

            index++;
            if (argumentType.equals(Type.DOUBLE_TYPE) || argumentType.equals(Type.LONG_TYPE)) {
                //types.put(index, Type.VOID_TYPE);
                index++;
            }
        }

        for (Local local : builder.locals) {
            final Type type = types.get(local.getCodeIndex());

            if (type == null)
                continue;

            local.setType(type);
        }

        builder.graph
                .allExprStream()
                .filter(VarExpr.class::isInstance)
                .map(VarExpr.class::cast)
                .forEach(e -> {
                    final Type type = types.get(e.getIndex());

                    if (type == null)
                        return;

                    e.setType(type);
                });

        builder.graph
                .vertices()
                .stream()
                .flatMap(BasicBlock::stream)
                .filter(CopyVarStmt.class::isInstance)
                .map(CopyVarStmt.class::cast)
                .map(CopyVarStmt::getVariable)
                .forEach(e -> {
                    final Type type = types.get(e.getIndex());

                    if (type == null)
                        return;

                    e.setType(type);
                });
    }
}
