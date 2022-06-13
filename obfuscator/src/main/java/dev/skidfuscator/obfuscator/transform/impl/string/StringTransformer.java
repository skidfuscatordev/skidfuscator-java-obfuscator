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
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
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

        cfg.allExprStream()
                /*
                 *
                 */
                .filter(SkidConstantExpr.class::isInstance)
                .map(ConstantExpr.class::cast)
                .filter(constantExpr -> constantExpr.getConstant() instanceof String)
                /*
                 * We collect since we're modifying the expression stream
                 * we kinda need to just not cause any concurrency issue.
                 * ¯\_(ツ)_/¯
                 */
                .collect(Collectors.toList())
                .forEach(unit -> {
                    final CodeUnit parent = unit.getParent();

                    final String constant = (String) unit.getConstant();
                    final int value = methodNode.getBlockPredicate((SkidBlock) unit.getBlock());
                    final String encrypted = BasicEncryptionGenerator.encrypt(constant, value, keys);

                    final ConstantExpr encryptedExpr = new ConstantExpr(encrypted);
                    final Expr loadExpr = methodNode.getFlowPredicate().getGetter().get(unit.getBlock());

                    final Expr modified = new StaticInvocationExpr(new Expr[]{encryptedExpr, loadExpr},
                            methodNode.getOwner(), BasicEncryptionGenerator.METHOD_NAME,
                            "(Ljava/lang/String;I)Ljava/lang/String;");

                    try {
                        parent.overwrite(unit, modified);
                    } catch (IllegalStateException e) {
                        return;
                    }
                });
    }
}
