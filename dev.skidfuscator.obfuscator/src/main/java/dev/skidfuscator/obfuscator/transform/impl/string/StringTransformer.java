package dev.skidfuscator.obfuscator.transform.impl.string;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.builder.SkidMethodNodeBuilder;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidConstantExpr;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.BytesEncryptionGenerator;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.EncryptionGenerator;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.algo.AESEncryptionGenerator;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.algo.CaesarEncryptionGenerator;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.basic.BasicEncryptionGenerator;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.FieldNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.NewArrayExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class StringTransformer extends AbstractTransformer {
    private final Map<ClassNode, EncryptionGenerator> keyMap = new HashMap<>();

    private final Set<String> INJECTED = new HashSet<>();

    public StringTransformer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public StringTransformer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator, "String Encryption", children);
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        if (methodNode.isAbstract()
                || methodNode.isInit())
            return;

        if (methodNode.node.instructions.size() > 10000)
            return;

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        final SkidClassNode parentNode = methodNode.getParent();

        EncryptionGenerator generator = keyMap.get(parentNode);

        if (generator == null) {
            switch (RandomUtil.nextInt(1)) {
                case 1: {
                    final String iv = RandomUtil.randomAlphabeticalString(16);
                    keyMap.put(parentNode, (generator = new AESEncryptionGenerator(iv)));
                    break;
                }
                case 2: {
                    final int size = RandomUtil.nextInt(127) + 1;
                    final Integer[] keys = new Integer[size];

                    for (int i = 0; i < size; i++) {
                        keys[i] = RandomUtil.nextInt(127) + 1;
                    }

                    keyMap.put(parentNode, (generator = new CaesarEncryptionGenerator(keys)));
                    break;
                }
                default: {
                    final int size = RandomUtil.nextInt(127) + 1;
                    final Integer[] keys = new Integer[size];

                    for (int i = 0; i < size; i++) {
                        keys[i] = RandomUtil.nextInt(127) + 1;
                    }

                    keyMap.put(parentNode, (generator = new BytesEncryptionGenerator(keys)));
                    break;
                }
            }
        }

        if (!INJECTED.contains(parentNode.getName())) {
            generator.visit((SkidClassNode) methodNode.owner, BasicEncryptionGenerator.METHOD_NAME);
            INJECTED.add(parentNode.getName());
        }

        EncryptionGenerator finalGenerator = generator;
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
                    final byte[] encrypted = finalGenerator.encrypt(constant, value);

                    final SkidMethodNode injector = new SkidMethodNodeBuilder(skidfuscator, parentNode)
                            .access(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)
                            .name(RandomUtil.randomAlphabeticalString(15))
                            .desc("()[B")
                            .phantom(true)
                            .build();


                    final Expr[] csts = new Expr[encrypted.length];
                    for (int i = 0; i < encrypted.length; i++) {
                        csts[i] = new ConstantExpr(encrypted[i], Type.BYTE_TYPE);
                    }

                    final NewArrayExpr encryptedExpr = new NewArrayExpr(
                            new Expr[]{new ConstantExpr(encrypted.length, Type.INT_TYPE)},
                            Type.getType(byte[].class),
                            csts
                    );

                    injector.getCfg()
                            .getEntry()
                            .add(new ReturnStmt(Type.getType(byte[].class), encryptedExpr));

                    final Expr loadExpr = methodNode.getFlowPredicate().getGetter().get(unit.getBlock());
                    final Expr modified = new StaticInvocationExpr(new Expr[]{new StaticInvocationExpr(
                            new Expr[0],
                            parentNode.getName(),
                            injector.getName(),
                            injector.getDesc()
                    ), loadExpr},
                            methodNode.getOwner(), BasicEncryptionGenerator.METHOD_NAME,
                            "([BI)Ljava/lang/String;");

                    try {
                        parent.overwrite(unit, modified);
                    } catch (IllegalStateException e) {
                        return;
                    }
                });
    }
}
