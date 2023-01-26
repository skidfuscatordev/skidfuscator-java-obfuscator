package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.ir.cfg.builder.GenerationPass;
import org.mapleir.ir.cfg.builder.GenerationPassV2;
import org.mapleir.ir.code.ExpressionStack;
import org.mapleir.ir.code.expr.CaughtExceptionExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.*;
import java.util.stream.Collectors;

public class SkidGenerationPass extends GenerationPass {
    private final Skidfuscator skidfuscator;

    public SkidGenerationPass(ControlFlowGraphBuilder builder, Skidfuscator skidfuscator) {
        super(builder);
        this.skidfuscator = skidfuscator;
    }

    @Override
    public void init() {
        entry(checkLabel());

        Map<LabelNode, Set<Type>> handlers = new HashMap<>();
        for(TryCatchBlockNode tc : builder.method.node.tryCatchBlocks) {
            if(tc.start == tc.end) {
                continue;
            }

            this.handler(tc);
            handlers.computeIfAbsent(tc.handler, e -> new HashSet<>());

            if (tc.type == null)
                continue;

            handlers.get(tc.handler).add(Type.getType("L" + tc.type + ";"));

        }
        for(Map.Entry<LabelNode, Set<Type>> handler : handlers.entrySet()) {
            //System.out.println("Iterating types " + Arrays.toString(handler.getValue().toArray()));
            final Set<ClassNode> classNodes = handler
                    .getValue()
                    .stream()
                    .filter(e -> {
                        final ClassNode s = skidfuscator.getClassSource().findClassNode(e.getClassName().replace(".", "/"));
                        if (s == null) {
                            Skidfuscator.LOGGER.warn("Failed to find " + e.getInternalName());
                            return false;
                        }

                        return true;
                    })
                    .map(e -> skidfuscator.getClassSource().findClassNode(e.getClassName().replace(".", "/")))
                    .collect(Collectors.toSet());

            //System.out.println("Trying to find common ancestor of " + classNodes.stream().map(ClassNode::getName).collect(Collectors.joining(", ")));
            final Type type = classNodes.isEmpty()
                    ? Type.getType(Throwable.class)
                    : Type.getType(
                    "L" + skidfuscator.getClassSource()
                    .getClassTree()
                    .getCommonAncestor(classNodes)
                    .iterator()
                    .next().getName() + ";"
            );

            //System.out.println("Creating handler of target " + handler.getKey() + " of types "
            //        + Arrays.toString(handler.getValue().toArray()) + " of common " + type);

            this.handler(handler.getKey(), type);
        }
    }

    @Override
    protected void handler(TryCatchBlockNode tc) {
        marks.add(tc.start);
        marks.add(tc.end);
    }

    private void handler(final LabelNode label, Type extype) {
        BasicBlock handler = resolveTarget(label);

        if(getInputStackFor(handler) != null) {
//			System.err.println(handler.getInputStack());
//			System.err.println("Double handler: " + handler.getId() + " " + tc);
            return;
        }

        ExpressionStack stack = new ExpressionStack(16);
        setInputStack(handler, stack);

        CaughtExceptionExpr expr = new CaughtExceptionExpr(extype);
        Type type = expr.getType();
        VarExpr var = _var_expr(0, type, true);
        CopyVarStmt stmt = copy(var, expr, handler);
        handler.add(stmt);

        stack.push(load_stack(0, type));

        queue(label);

        stacks.set(handler, true);
    }


}
