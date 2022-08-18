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
import dev.skidfuscator.obfuscator.transform.impl.string.generator.BasicEncryptionGenerator;
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
    private final Map<ClassNode, BasicEncryptionGenerator> keyMap = new HashMap<>();
    private final Set<String> INJECTED = new HashSet<>();

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

        final ClassNode parentNode = methodNode.getParent();

        BasicEncryptionGenerator generator = keyMap.get(parentNode);

        if (generator == null) {
            final int size = RandomUtil.nextInt(127) + 1;
            final Integer[] keys = new Integer[size];

            for (int i = 0; i < size; i++) {
                keys[i] = RandomUtil.nextInt(127) + 1;
            }

            keyMap.put(parentNode, (generator = new BasicEncryptionGenerator(keys)));
        }

        if (!INJECTED.contains(parentNode.getName())) {
            generator.visit((SkidClassNode) methodNode.owner, BasicEncryptionGenerator.METHOD_NAME);
            INJECTED.add(parentNode.getName());
        }

        BasicEncryptionGenerator finalGenerator = generator;
        cfg.allExprStream()
                /*
                 *
                 */
                .filter(SkidConstantExpr.class::isInstance)
                .map(SkidConstantExpr.class::cast)
                .filter(e -> !e.isExempt())
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
                    final String encrypted = finalGenerator.encrypt(constant, value);

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
