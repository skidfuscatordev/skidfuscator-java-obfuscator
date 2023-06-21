package dev.skidfuscator.obfuscator.transform.impl.flow.driver;

import dev.skidfuscator.config.DefaultTransformerConfig;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.InitSkidTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.PostSkidTransformEvent;
import dev.skidfuscator.obfuscator.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obfuscator.predicate.cache.CacheTemplateDump;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.MiscUtil;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.asm.ClassHelper;
import org.mapleir.flowgraph.edges.ImmediateEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.PopStmt;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.topdank.byteengineer.commons.data.JarClassData;

public class DriverTransformer extends AbstractTransformer {
    public DriverTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Driver");
    }

    private SkidClassNode predicateCache;
    private SkidMethodNode predicateNode;
    private BasicBlock currentBlock;
    private final XorNumberTransformer numberTransformer = new XorNumberTransformer();


    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        final boolean exempt = methodNode.owner.node.outerClass != null
                || methodNode.owner.node.nestHostClass != null
                || methodNode.owner.isPrivate()
                || (methodNode.owner.node.innerClasses != null
                && methodNode.owner.node.innerClasses
                .stream().anyMatch(e -> e.name.equals(methodNode.owner.node.name)));

        if (exempt) {
            this.skip();
            return;
        }

        final MethodOpaquePredicate methodPredicate = methodNode.getPredicate();
        methodPredicate.setGetter(new PredicateFlowGetter() {
            @Override
            public Expr get(BasicBlock vertex) {
                final SkidMethodNode skidMethodNode = (SkidMethodNode) vertex.cfg.getMethodNode();
                final SkidClassNode skidClassNode = (SkidClassNode) skidMethodNode.owner;

                final ClassOpaquePredicate classPredicate = skidMethodNode.isStatic()
                        ? skidMethodNode.getParent().getStaticPredicate()
                        : skidMethodNode.getParent().getClassPredicate();
                int seed;
                PredicateFlowGetter expr;
                if (skidMethodNode.isClinit() || skidMethodNode.isInit()) {
                    final int randomSeed = skidClassNode.getRandomInt();
                    final String randomString = RandomUtil.randomAlphabeticalString(16);

                    seed = randomSeed;

                    expr = new PredicateFlowGetter() {
                        @Override
                        public Expr get(BasicBlock vertex) {
                            if (methodNode.isClinit())
                                return new StaticInvocationExpr(
                                        new Expr[]{
                                                new ConstantExpr(randomString)
                                        },
                                        "skid/Driver",
                                        "get",
                                        "(Ljava/lang/String;)I"
                                );
                            else
                                return numberTransformer.getNumber(
                                        randomSeed,
                                        skidClassNode.getStaticPredicate().get(),
                                        vertex,
                                        skidClassNode.getStaticPredicate().getGetter()
                                );
                        }
                    };

                    if (methodNode.isClinit()) {
                        final BasicBlock block = new BasicBlock(predicateNode.getCfg());
                        predicateNode.getCfg().getEntries().remove(predicateNode.getCfg().getEntry());
                        predicateNode.getCfg().getEntries().add(block);
                        predicateNode.getCfg().addVertex(block);
                        predicateNode.getCfg().addEdge(new ImmediateEdge<>(
                                block,
                                currentBlock
                        ));

                        currentBlock = block;
                        block.add(new PopStmt(
                                new StaticInvocationExpr(
                                        new Expr[]{
                                                new ConstantExpr(randomString),
                                                new ConstantExpr(seed, Type.INT_TYPE)
                                        },
                                        "skid/Driver",
                                        "add",
                                        "(Ljava/lang/String;I)V"
                                )
                        ));
                    }
                } else {
                    seed = classPredicate.get();
                    expr = classPredicate.getGetter();
                }

                if (!skidMethodNode.getGroup().isEntryPoint()) {
                    seed = seed ^ skidMethodNode.getGroup().getPredicate().getPublic();

                    final PredicateFlowGetter previousExprGetter = expr;
                    expr = new PredicateFlowGetter() {
                        @Override
                        public Expr get(BasicBlock vertex) {
                            final ControlFlowGraph cfg = vertex.getGraph();

                            return new ArithmeticExpr(
                                    /* Get the seed from the parameter */
                                    new VarExpr(
                                            cfg.getLocals().get(skidMethodNode.getGroup().getStackHeight()),
                                            Type.INT_TYPE
                                    ),
                                    /* Hash the previous instruction */
                                    previousExprGetter.get(vertex),
                                    /* Obv xor operation */
                                    ArithmeticExpr.Operator.XOR
                            );
                        }
                    };
                }

                return numberTransformer.getNumber(
                        methodPredicate.getPrivate(),
                        seed,
                        vertex,
                        expr
                );
            }
        });

        this.success();
    }

    @Listen
    void handle(final InitSkidTransformEvent event) {
        try {
            predicateCache = new SkidClassNode(
                    ClassHelper.create(CacheTemplateDump.dump(), 0).node,
                    skidfuscator
            );
            final String oldName = predicateCache.node.name;
            predicateCache.node.name = "skid/Driver";
            for (MethodNode method : predicateCache.node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode) {
                        final MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                        if (methodInsnNode.owner.equals(oldName)) {
                            methodInsnNode.owner = "skid/Driver";
                        }
                    }

                    else if (instruction instanceof FieldInsnNode) {
                        final FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
                        if (fieldInsnNode.owner.equals(oldName)) {
                            fieldInsnNode.owner = "skid/Driver";
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.fail();
            Skidfuscator.LOGGER.error("Failed to create dispatcher. This WILL cause issues!", e);
            return;
        }

        final String name = this.getConfig().getName();

        skidfuscator.getClassSource().add(predicateCache);
        skidfuscator
                .getJarContents()
                .getClassContents()
                .add(
                        new JarClassData(
                                name + ".class",
                                predicateCache.toByteArray(),
                                predicateCache
                        )
                );
        skidfuscator.getClassRemapper().add(
                "skid/Driver",
                name
        );

        predicateNode = (SkidMethodNode) predicateCache.getMethods()
                .stream()
                .filter(e -> e.getName().equals("init"))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        currentBlock = predicateNode.getCfg().getEntry();
    }

    @Listen
    void handle(final PostSkidTransformEvent event) {
        predicateNode.dump();
    }

    @Override
    protected <T extends DefaultTransformerConfig> T createConfig() {
        return (T) new DriverConfig(skidfuscator.getTsConfig(), MiscUtil.toCamelCase(name));
    }

    @Override
    public DriverConfig getConfig() {
        return (DriverConfig) super.getConfig();
    }
}