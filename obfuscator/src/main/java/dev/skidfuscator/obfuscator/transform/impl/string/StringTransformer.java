package dev.skidfuscator.obfuscator.transform.impl.string;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidConstantExpr;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;

import java.util.*;
import java.util.stream.Collectors;

public class StringTransformer extends AbstractTransformer {
    private final Map<ClassNode, Integer[]> keyMap = new HashMap<>();
    private final Set<ClassNode> INJECTED = new HashSet<>();

    public StringTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public StringTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator, "String Transformer", children);
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

        final ClassNode parentNode = methodNode.owner;

        Integer[] keysT = keyMap.get(parentNode);

        if (keysT == null) {
            final int size = RandomUtil.nextInt(128) + 1;
            keysT = new Integer[size];
            for (int i = 0; i < size; i++) {
                keysT[i] = RandomUtil.nextInt(128);
            }

            keyMap.put(parentNode, keysT);
        }

        final Integer[] keys = keysT;

        if (!INJECTED.contains(parentNode)) {
            BasicEncryptionGenerator.visit((SkidClassNode) methodNode.owner, BasicEncryptionGenerator.METHOD_NAME, keys);
            INJECTED.add(parentNode);
        }
        // Overwrites constants in method arguments
        cfg.allExprStream().filter(InvocationExpr.class::isInstance).map(InvocationExpr.class::cast).forEach(expr -> {
            for (Expr arg : expr.getArgumentExprs()) {
                if (arg instanceof ConstantExpr && ((ConstantExpr) arg).getConstant() instanceof String) {
                    overWriteConstant(methodNode, (ConstantExpr) arg, keys);
                }
            }
        });
        // Overwrites constants
        cfg.allExprStream()
                .filter(SkidConstantExpr.class::isInstance)
                .map(ConstantExpr.class::cast)
                .filter(constantExpr -> constantExpr.getConstant() instanceof String)
                .collect(Collectors.toList())
                .forEach(unit -> {
                    overWriteConstant(methodNode, unit, keys);
                });
    }
    public void overWriteConstant(SkidMethodNode methodNode, ConstantExpr expr, Integer[] keys) {
        final CodeUnit parent = expr.getParent();

        final String constant = (String) expr.getConstant();
        final int value = methodNode.getBlockPredicate((SkidBlock) expr.getBlock());
        final String encrypted = BasicEncryptionGenerator.encrypt(constant, value, keys);

        final ConstantExpr encryptedExpr = new ConstantExpr(encrypted);
        final Expr loadExpr = methodNode.getFlowPredicate().getGetter().get(expr.getBlock());

        final Expr modified = new StaticInvocationExpr(new Expr[]{encryptedExpr, loadExpr},
                methodNode.getOwner(), BasicEncryptionGenerator.METHOD_NAME,
                "(Ljava/lang/String;I)Ljava/lang/String;");

        try {
            parent.overwrite(expr, modified);
        } catch (IllegalStateException e) {
            return;
        }
    }
}
