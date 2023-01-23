package dev.skidfuscator.obfuscator.predicate.renderer;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.clazz.InitClassTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.group.InitGroupTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.PostMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.InitSkidTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.PostSkidTransformEvent;
import dev.skidfuscator.obfuscator.hierarchy.matching.ClassMethodHash;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obfuscator.predicate.cache.CacheTemplateDump;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.ConditionalJumpRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.SwitchJumpRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.UnconditionalJumpRenderer;
import dev.skidfuscator.obfuscator.skidasm.*;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpStmt;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeUnconditionalJumpStmt;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.util.OpcodeUtil;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.*;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.*;
import org.mapleir.ir.code.stmt.*;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.topdank.byteengineer.commons.data.JarClassData;

import java.util.*;
import java.util.stream.Collectors;

public class IntegerBlockPredicateRenderer extends AbstractTransformer {
    public IntegerBlockPredicateRenderer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"GEN3 Flow", children);
    }

    public static boolean DEBUG = false;
    private SkidClassNode predicateCache;
    private SkidMethodNode predicateNode;
    private BasicBlock currentBlock;

    @Listen
    void handle(final InitSkidTransformEvent event) {
        if (!skidfuscator.getConfig().isDriver())
            return;

        predicateCache = new SkidClassNode(
                ClassHelper.create(CacheTemplateDump.dump(), 0).node,
                skidfuscator
        );

        skidfuscator.getClassSource().add(predicateCache);
        skidfuscator
                .getJarContents()
                .getClassContents()
                .add(
                        new JarClassData(
                                "skid/Driver.class",
                                predicateCache.toByteArray(),
                                predicateCache
                        )
                );
        skidfuscator.getClassRemapper().add(
                Type.getObjectType(predicateCache.getName()).getInternalName(),
                "skid/Driver"//ClassRenamerTransformer.DICTIONARY.next()
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
        if (!skidfuscator.getConfig().isDriver() || predicateNode == null)
            return;

        predicateNode.dump();
    }

    /**
     * Method called when the class methods are iterated over and initialized.
     * In this we'll set the flow obfuscation opaque predicate getter and setter.
     *
     * @param event Method initializer event
     */
    @Listen
    void handle(final InitMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        final BlockOpaquePredicate flowPredicate = methodNode.getFlowPredicate();

        if (methodNode.getName().equals("lambda$null$5") && methodNode.owner.getName().contains("PlayerListener")) {
            System.out.println(methodNode.getCfg().toString());
        }

        /*
         * This local is the initial local which stores the opaque predicate.
         * This will constantly be called and will perpetually store the
         * opaque predicate's value which changes each block.
         *
         * The reason for the +3 is simple:
         * +1 -> Get the next local
         * +2 -> Get the next long local
         * +3 -> Safety net to allow for "long" to be used (I don't really know
         *       this fixed all my issues)
         */
        final Local local = methodNode
                .getCfg()
                .getLocals()
                .get(methodNode.getCfg().getLocals().getMaxLocals() + 3);
        local.setFlag(SkidBlock.FLAG_PROXY, true);
        methodNode.getEntryBlock();

        /*
         * The getter right now is quite simple:
         *
         * [if] the block is exempted (eg: <init> method calls before super())
         * -->  Return a constant expression with the predicate
         *
         * [else]
         * -->  Return a variable expression which calls the local storing the
         *      predicate
         */
        flowPredicate.setGetter(new PredicateFlowGetter() {
            @Override
            public Expr get(final BasicBlock block) {
                if (block.isFlagSet(SkidBlock.FLAG_NO_OPAQUE)) {
                    return new ConstantExpr(flowPredicate.get((SkidBlock) block), Type.INT_TYPE);
                }

                return new VarExpr(local, Type.INT_TYPE);
            }
        });

        flowPredicate.setSetter(new PredicateFlowSetter() {
            @Override
            public Stmt apply(Expr expr) {
                return new CopyVarStmt(new VarExpr(local, Type.INT_TYPE), expr);
            }
        });

        final MethodOpaquePredicate methodPredicate = methodNode.getPredicate();

        if (methodPredicate == null)
            return;

        methodPredicate.setGetter(new PredicateFlowGetter() {
            @Override
            public Expr get(BasicBlock vertex) {
                final XorNumberTransformer numberTransformer = new XorNumberTransformer();
                final SkidMethodNode skidMethodNode = (SkidMethodNode) vertex.cfg.getMethodNode();
                final SkidClassNode skidClassNode = (SkidClassNode) skidMethodNode.owner;

                final ClassOpaquePredicate classPredicate = skidMethodNode.isStatic()
                                ? skidMethodNode.getParent().getStaticPredicate()
                                : skidMethodNode.getParent().getPredicate();
                int seed;
                PredicateFlowGetter expr;
                if (skidMethodNode.isClinit() || skidMethodNode.isInit()) {
                    final int randomSeed = skidClassNode.getRandomInt();
                    seed = randomSeed;

                    final String randomString = RandomUtil.randomAlphabeticalString(16);

                    if (!skidfuscator.getConfig().isDriver()
                            || predicateNode == null
                            || methodNode.owner.node.outerClass != null
                            || methodNode.owner.node.nestHostClass != null
                            || methodNode.owner.isPrivate()
                            || (methodNode.owner.node.innerClasses != null
                                && methodNode.owner.node.innerClasses
                                .stream().anyMatch(e -> e.name.equals(methodNode.owner.node.name)))) {
                        expr = new PredicateFlowGetter() {
                            @Override
                            public Expr get(BasicBlock vertex) {
                                return new StaticInvocationExpr(
                                        new Expr[]{new ConstantExpr("" + randomSeed, TypeUtil.STRING_TYPE)},
                                        "java/lang/Integer",
                                        "parseInt",
                                        "(Ljava/lang/String;)I"
                                );
                            }
                        };
                    } else {
                        expr = new PredicateFlowGetter() {
                            @Override
                            public Expr get(BasicBlock vertex) {
                                return new StaticInvocationExpr(
                                        new Expr[]{
                                                new ConstantExpr(randomString)
                                        },
                                        "dev/skidfuscator/obfuscator/predicate/cache/CacheTemplate",
                                        "get",
                                        "(Ljava/lang/String;)I"
                                );
                            }
                        };

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
                                        "dev/skidfuscator/obfuscator/predicate/cache/CacheTemplate",
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
    }

    /**
     * This listener handles every class before they are transformed. The objective
     * here is to prepare any sort of opaque predicate stuff before it is used by
     * the transformers.
     *
     * @param event InitClassTransformEvent event containing any necessary ref
     */
    @Listen
    void handle(final InitClassTransformEvent event) {
        final SkidClassNode classNode = event.getClassNode();

        final ClassOpaquePredicate clazzInstancePredicate = classNode.getPredicate();
        final ClassOpaquePredicate clazzStaticPredicate = classNode.getStaticPredicate();

        /*
         * Both methods down below are designed to create a specific way to access
         * a class opaque predicate. To ensure the design is as compact and
         * feasible as humanly possible, we need to exempt some scenarios:
         *
         * if no methods are NON-static         -> access directly via constant expression
         * if class is interface or annotation  -> access directly via constant expression
         * else                                 -> access by loading the value of a field
         */
        final Runnable createConstantInstance = () -> {
            /*
             * Here the getter to access the value is a constant expression loading the
             * constant expr.
             *
             * The setter, on the other hand, throws an exception as it expresses a risk
             * that should be circumvented.
             */
            clazzInstancePredicate.setGetter(new PredicateFlowGetter() {
                @Override
                public Expr get(final BasicBlock vertex) {
                    // TODO: Do class instance opaque predicates
                    return new ConstantExpr(
                            clazzInstancePredicate.get(),
                            Type.INT_TYPE
                    );
                }
            });
            clazzInstancePredicate.setSetter(new PredicateFlowSetter() {
                @Override
                public Stmt apply(Expr expr) {
                    throw new IllegalStateException("Cannot set value for a constant getter");
                }
            });
        };
        final Runnable createDynamicInstance = () -> {
            /*
             * Here, we create a new field in which we'll store the value. As of right now
             * the value is loaded by default as a field constant. However, in the future,
             * this should be calling a specific method in a different class to complicate
             * the reverse engineering task.
             *
             * Contrary to the previous getter, this one store the value in a field. This
             * should make things harder for individuals to fuck around with.
             */
            final int troll = clazzInstancePredicate.get() ^ clazzStaticPredicate.get();
            final SkidFieldNode fieldNode = classNode
                    .createField()
                    .access(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT)
                    .name(RandomUtil.randomIsoString(10))
                    .desc("I")
                    .value(troll)
                    .build();

            clazzInstancePredicate.setGetter(new PredicateFlowGetter() {
                @Override
                public Expr get(final BasicBlock vertex) {
                    return new FieldLoadExpr(
                            /* Call the class this variable */
                            new VarExpr(
                                    vertex.cfg.getLocals().get(0),
                                    classNode.getType()
                            ),
                            classNode.node.name,
                            fieldNode.node.name,
                            fieldNode.node.desc,
                            false
                    );
                }
            });
            clazzInstancePredicate.setSetter(new PredicateFlowSetter() {
                @Override
                public Stmt apply(Expr expr) {
                    return new FieldStoreStmt(
                            new VarExpr(
                                    expr.getBlock().cfg.getLocals().get(0),
                                    Type.getType("L" + classNode.getName() + ";")
                            ),
                            expr,
                            classNode.node.name,
                            fieldNode.node.name,
                            fieldNode.node.desc,
                            false
                    );
                }
            });


            classNode.getMethods()
                    .stream()
                    .filter(MethodNode::isInit)
                    .filter(SkidMethodNode.class::isInstance)
                    .map(SkidMethodNode.class::cast)
                    .forEach(skidMethodNode -> {
                        final Expr expr = new FakeArithmeticExpr(
                                new ConstantExpr(troll, Type.INT_TYPE),
                                clazzStaticPredicate.getGetter().get(skidMethodNode.getEntryBlock()),
                                ArithmeticExpr.Operator.XOR
                        );
                        expr.setBlock(skidMethodNode.getEntryBlock());
                        skidMethodNode.getEntryBlock().add(
                                0,
                                clazzInstancePredicate.getSetter().apply(expr)
                        );
                    });
        };

        /*
         * Both methods down below are designed to create a specific way to access
         * a STATIC class opaque predicate. To ensure the design is as compact and
         * feasible as humanly possible, we need to exempt some scenarios:
         *
         * if no methods are static             -> access directly via constant expression
         * if class is interface or annotation  -> access directly via constant expression
         * else                                 -> access by loading the value of a field
         */
        final Runnable createConstantStatic = () -> {
            clazzStaticPredicate.setGetter(new PredicateFlowGetter() {
                @Override
                public Expr get(final BasicBlock vertex) {
                    // TODO: Do class instance opaque predicates
                    return new ConstantExpr(
                            clazzStaticPredicate.get(),
                            Type.INT_TYPE
                    );
                }
            });
            clazzStaticPredicate.setSetter(new PredicateFlowSetter() {
                @Override
                public Stmt apply(Expr expr) {
                    throw new IllegalStateException("Cannot set value for a constant getter");
                }
            });
        };

        final Runnable createDynamicStatic2 = () -> {
            final SkidFieldNode staticFieldNode = classNode.createField()
                    .access(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)
                    .name(RandomUtil.randomIsoString(10))
                    .desc("I")
                    .value(0)
                    .build();

            final SkidMethodNode clinit = classNode.getClassInit();

            final long seed = RandomUtil.nextLong();
            final Random random = new Random(seed);
            final int nextInt = random.nextInt();

            final Local local = clinit
                    .getCfg()
                    .getLocals()
                    .get(clinit.getCfg().getLocals().getMaxLocals() + 3);
            local.setFlag(SkidBlock.FLAG_PROXY, true);

            clinit.getEntryBlock().add(
                    0,
                    new SkidCopyVarStmt(
                            new VarExpr(local, Type.INT_TYPE),
                            new VirtualInvocationExpr(
                                    InvocationExpr.CallType.VIRTUAL,
                                    new Expr[]{
                                            new InitialisedObjectExpr(
                                                    "java/util/Random",
                                                    "(J)V",
                                                    new Expr[]{
                                                            new ConstantExpr(seed, Type.LONG_TYPE)
                                                    }
                                            )
                                    },
                                    "java/util/Random",
                                    "nextInt",
                                    "()I"
                            )

                    )
            );

            clinit.getEntryBlock().add(
                    1,
                    new FieldStoreStmt(
                        null,
                        new XorNumberTransformer().getNumber(
                                clazzStaticPredicate.get(),
                                nextInt,
                                clinit.getEntryBlock(),
                                new PredicateFlowGetter() {
                                    @Override
                                    public Expr get(BasicBlock vertex) {
                                        return new VarExpr(local, Type.INT_TYPE);
                                    }
                                }
                        ),
                        classNode.getName(),
                        staticFieldNode.getName(),
                        staticFieldNode.getDesc(),
                        true
                    )
            );

            clazzStaticPredicate.setGetter(new PredicateFlowGetter() {
                @Override
                public Expr get(final BasicBlock vertex) {
                    return new FieldLoadExpr(
                            null,
                            classNode.node.name,
                            staticFieldNode.node.name,
                            staticFieldNode.node.desc,
                            true
                    );
                }
            });
            clazzStaticPredicate.setSetter(new PredicateFlowSetter() {
                @Override
                public Stmt apply(Expr expr) {
                    return new FieldStoreStmt(
                            null,
                            expr,
                            classNode.node.name,
                            staticFieldNode.node.name,
                            staticFieldNode.node.desc,
                            true
                    );
                }
            });
        };

        final boolean skip = classNode.isInterface()
                || classNode.isAnnotation()
                || classNode.isEnum();

        if (skip) {
            createConstantStatic.run();
            createConstantInstance.run();
        }

        else {
            final boolean addStatic = classNode.getMethods()
                    .stream()
                    .anyMatch(e -> !e.isClinit() && e.isStatic());

            if (addStatic) {
                createDynamicStatic2.run();
            } else {
                createConstantStatic.run();
            }

            final boolean addInstance = classNode.getMethods()
                    .stream()
                    .anyMatch(e -> !e.isInit() && !e.isStatic());

            // TODO: Figure out why tf this breaks shit
            if (addInstance) {
                createDynamicInstance.run();
            } else {
                createConstantInstance.run();
            }
        }
    }

    @Listen
    void handle(final InitGroupTransformEvent event) {
        final SkidGroup skidGroup = event.getGroup();

        /*
         * This can occur. Warn the user then skip. No significant damage
         * should be caused by skipping this method.
         *
         * TODO: Add stricter exception logging for this
         */
        if (skidGroup.getPredicate().getGetter() != null) {
            System.err.println("SkidGroup " + skidGroup.getName() + " does not have getter!");
            return;
        }

        final boolean entryPoint = skidGroup.isEntryPoint();
        int stackHeight = -1;

        /*
         * If the skid group is an entry point (it has no direct invocation)
         * or in the future when we support reflection calls
         */
        if (entryPoint) {
            stackHeight = OpcodeUtil.getArgumentsSizes(skidGroup.getDesc());

            if (skidGroup.isStatical())
                stackHeight -= 1;

            skidGroup.setStackHeight(stackHeight);
            return;
        }

        Local local = null;
        String desc = null;

        int indexGroup = -1;

        //System.out.println("Iterating group " + skidGroup.getName());

        final Parameter parameterGroup = new Parameter(skidGroup.getDesc());
        if (skidGroup.getInvokers().stream().map(SkidInvocation::getExpr)
                .anyMatch(DynamicInvocationExpr.class::isInstance)) {
            final DynamicInvocationExpr invocationExpr = skidGroup
                    .getInvokers()
                    .stream()
                    .map(SkidInvocation::getExpr)
                    .filter(DynamicInvocationExpr.class::isInstance)
                    .findFirst()
                    .map(DynamicInvocationExpr.class::cast)
                    .orElseThrow(IllegalStateException::new);

            final Parameter bootstrappedParam = new Parameter(
                    invocationExpr.getDesc()
            );
            indexGroup = bootstrappedParam.getArgs().size();
        } else {
            indexGroup = parameterGroup.getArgs().size();
        }

        parameterGroup.insertParameter(Type.INT_TYPE, indexGroup);

        for (MethodNode methodNode : skidGroup.getMethodNodeList()) {
            final SkidMethodNode skidMethodNode = (SkidMethodNode) methodNode;

            stackHeight = parameterGroup.computeSize(indexGroup);
            if (!methodNode.isStatic()) stackHeight += 1;

            final Map<String, Local> localMap = new HashMap<>();
            for (Map.Entry<String, Local> stringLocalEntry :
                    skidMethodNode.getCfg().getLocals().getCache().entrySet()) {
                final String old = stringLocalEntry.getKey();
                final String oldStringId = old.split("var")[1].split("_")[0];
                final int oldId = Integer.parseInt(oldStringId);

                if (oldId < stackHeight) {
                    localMap.put(old, stringLocalEntry.getValue());
                    continue;
                }
                final int newId = oldId + 1;

                final String newVar = old.replace("var" + oldStringId, "var" + Integer.toString(newId));
                stringLocalEntry.getValue().setIndex(stringLocalEntry.getValue().getIndex() + 1);
                localMap.put(newVar, stringLocalEntry.getValue());
            }

            skidMethodNode.getCfg().getLocals().getCache().clear();
            skidMethodNode.getCfg().getLocals().getCache().putAll(localMap);

            methodNode.node.desc = desc = parameterGroup.getDesc();

            final ClassMethodHash classMethodHash = new ClassMethodHash(skidMethodNode);

            if (skidfuscator.getHierarchy().getGroup(classMethodHash) != null && !skidGroup.getName().contains("<")) {
                skidGroup.setName(skidGroup.getName() + "$" + RandomUtil.nextInt());
            }

            if (local == null) {
                local = skidMethodNode.getCfg().getLocals().get(stackHeight);
            }
        }

        if (!skidGroup.getInvokers().isEmpty()) {
            for (SkidInvocation invoker : skidGroup.getInvokers()) {
                assert invoker != null : String.format("Invoker %s is null!", Arrays.toString(skidGroup.getInvokers().toArray()));

                if (invoker.isTainted()) {
                    Skidfuscator.LOGGER.warn("Warning! Almost duplicated call on " + invoker.asExpr().toString());
                    continue;
                }

                //if (skidGroup.getName().equals("getConfig"))
                //    System.out.println("Replacing invoker " + invoker.asExpr().getOwner() + "#" + invoker.asExpr().getName() + invoker.asExpr().getDesc() + " in " + invoker.getOwner().toString());

                assert invoker.getExpr() != null : String.format("Invoker %s is null!", invoker.getOwner().getDisplayName());
                final boolean isDynamic = invoker.getExpr() instanceof DynamicInvocationExpr;

                int index = 0;
                final Expr[] params = /*isDynamic
                    ? ((DynamicInvocationExpr) invoker.getExpr()).getPrintedArgs()
                    : */invoker.getExpr().getArgumentExprs();
                for (Expr argumentExpr : params) {
                    assert argumentExpr != null : "Argument of index " + index + " is null!";
                    index++;
                }

                final Expr[] args = new Expr[params.length + 1];
                System.arraycopy(
                        params,
                        0,
                        args,
                        0,
                        params.length
                );

                final ConstantExpr constant = new ConstantExpr(skidGroup.getPredicate().getPublic());
                args[args.length - 1] = constant;

                invoker.getExpr().setArgumentExprs(args);
                invoker.setTainted(true);

                if (isDynamic) {
                    final Handle boundFunc = (Handle) ((DynamicInvocationExpr) invoker.getExpr()).getBootstrapArgs()[1];
                    final Parameter handlerDesc = new Parameter(boundFunc.getDesc());
                    handlerDesc.insertParameter(Type.INT_TYPE, indexGroup);
                    final Handle newBoundFunc = new Handle(boundFunc.getTag(), boundFunc.getOwner(), boundFunc.getName(),
                            handlerDesc.getDesc(), boundFunc.isInterface());

                    final Parameter parameter = new Parameter(invoker.getExpr().getDesc());
                    parameter.insertParameter(Type.INT_TYPE, indexGroup);
                    System.out.println("-----[ " + boundFunc.getOwner() + "#" + boundFunc.getName() + " ]-----");
                    System.out.println("\n" + Arrays.stream(((DynamicInvocationExpr) invoker.getExpr()).getArgumentExprs()).map(Expr::getType).map(Object::toString).collect(Collectors.joining("\n")) + "\n");
                    System.out.println("\n" + Arrays.stream(((DynamicInvocationExpr) invoker.getExpr()).getBootstrapArgs()).map(Object::toString).collect(Collectors.joining("\n")) + "\n");
                    System.out.println(invoker.getExpr().getDesc()  + " new: " + parameter.getDesc());
                    System.out.println(boundFunc.getDesc() + " new " + newBoundFunc.getDesc());
                    invoker.getExpr().setDesc(parameter.getDesc());

                    ((DynamicInvocationExpr) invoker.getExpr()).getBootstrapArgs()[1] = newBoundFunc;
                } else {
                    invoker.getExpr().setDesc(desc);
                }
            }
        }

        final int finalStackHeight = stackHeight;
        skidGroup.setStackHeight(finalStackHeight);

    }

    @Listen
    void handle(final PostMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();
        final ControlFlowGraph cfg = methodNode.getCfg();
        final BasicBlock entryPoint = methodNode.getEntryBlock();
        final SkidBlock seedEntry = (SkidBlock) entryPoint;
        cfg.recomputeEdges();
        cfg.verify();

        /*
         *    ____     __
         *   / __/__  / /_______ __
         *  / _// _ \/ __/ __/ // /
         * /___/_//_/\__/_/  \_, /
         *                  /___/
         */
        /*
         * Create a stack local to temporarily store the seed getter
         */
        final MethodOpaquePredicate predicate = methodNode.getPredicate();
        PredicateFlowGetter getter = predicate.getGetter();

        assert getter != null : "Predicate flow getter is null! Has group not been initialized?";

        PredicateFlowGetter localGetterT = methodNode.getFlowPredicate().getGetter();
        PredicateFlowSetter localSetterT = methodNode.getFlowPredicate().getSetter();

        // TODO: Figure out why this is happening too
        assert localGetterT != null : "Local getter for flow is absent";
        assert localSetterT != null : "Local setter for flow is absent";

        final PredicateFlowGetter localGetter = localGetterT;
        final PredicateFlowSetter localSetter = localSetterT;

        /*
         * Here we transition the method
         */
        final Expr loadedChanged = /*new ConstantExpr(seedEntry.getSeed(), Type.INT_TYPE); */
                new XorNumberTransformer().getNumber(
                        methodNode.getBlockPredicate(seedEntry), // Outcome
                        methodNode.getPredicate().getPrivate(), // Entry
                        entryPoint,
                        getter
                );

        final Stmt copyVarStmt = localSetter.apply(loadedChanged);
        entryPoint.add(0, copyVarStmt);
        if (DEBUG) {
            final int seed = methodNode.getBlockPredicate(seedEntry);
            final BasicBlock exception = Blocks.exception(cfg, "Failed to match seed of value " + seed);
            final Stmt jumpStmt = new FakeConditionalJumpStmt(
                    methodNode.getFlowPredicate().getGetter().get(entryPoint),
                    new ConstantExpr(seed, Type.INT_TYPE),
                    exception,
                    ConditionalJumpStmt.ComparisonType.NE
            );
            cfg.addEdge(new ConditionalJumpEdge<>(entryPoint, exception, Opcodes.IFNE));
            entryPoint.add(1, jumpStmt);
        }

        if (DEBUG) {
            final int seed = methodNode.getPredicate().getPrivate();
            final BasicBlock exception = Blocks.exception(cfg, "Failed to match entry seed of value " + seed + " of entry " + methodNode.getPredicate().getPublic());
            final Stmt jumpStmt = new FakeConditionalJumpStmt(
                    methodNode.getPredicate().getGetter().get(entryPoint),
                    new ConstantExpr(seed, Type.INT_TYPE),
                    exception,
                    ConditionalJumpStmt.ComparisonType.NE
            );
            cfg.addEdge(new ConditionalJumpEdge<>(entryPoint, exception, Opcodes.IFNE));
            entryPoint.add(0, jumpStmt);
        }


        /*
         *    __  __                           ___ __  _                   __
         *   / / / /___  _________  ____  ____/ (_) /_(_)___  ____  ____ _/ /
         *  / / / / __ \/ ___/ __ \/ __ \/ __  / / __/ / __ \/ __ \/ __ `/ /
         * / /_/ / / / / /__/ /_/ / / / / /_/ / / /_/ / /_/ / / / / /_/ / /
         * \____/_/ /_/\___/\____/_/ /_/\__,_/_/\__/_/\____/_/ /_/\__,_/_/
         *
         */
        for (BasicBlock vertex : new HashSet<>(cfg.vertices())) {
            if (vertex instanceof FakeBlock)
                continue;

            new HashSet<>(cfg.getEdges(vertex))
                    .stream()
                    .filter(e -> e instanceof ImmediateEdge)
                    .forEach(e -> {
                        this.addSeedLoader(
                                e.src(),
                                e.dst(),
                                e.src().size(),
                                localGetter,
                                localSetter,
                                methodNode.getBlockPredicate((SkidBlock) e.src()),
                                methodNode.getBlockPredicate((SkidBlock) e.dst()),
                                "Unconditional"
                        );
                    });
        }

        final InstructionRenderer<UnconditionalJumpStmt> unconditionalRenderer = new UnconditionalJumpRenderer();
        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            new HashSet<>(block)
                    .stream()
                    .filter(e -> e instanceof UnconditionalJumpStmt && !(e instanceof FakeUnconditionalJumpStmt))
                    .map(e -> (UnconditionalJumpStmt) e)
                    .forEach(stmt -> unconditionalRenderer.transform(skidfuscator, stmt));
        }

        /*
         *    ______                ___ __  _                   __
         *   / ____/___  ____  ____/ (_) /_(_)___  ____  ____ _/ /
         *  / /   / __ \/ __ \/ __  / / __/ / __ \/ __ \/ __ `/ /
         * / /___/ /_/ / / / / /_/ / / /_/ / /_/ / / / / /_/ / /
         * \____/\____/_/ /_/\__,_/_/\__/_/\____/_/ /_/\__,_/_/
         *
         */
        final InstructionRenderer<ConditionalJumpStmt> conditionRenderer = new ConditionalJumpRenderer();
        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            new HashSet<>(block)
                    .stream()
                    .filter(e -> e instanceof ConditionalJumpStmt && !(e instanceof FakeConditionalJumpStmt))
                    .map(e -> (ConditionalJumpStmt) e)
                    .forEach(stmt -> conditionRenderer.transform(skidfuscator, stmt));
        }
        /*
         *    _____         _ __       __
         *   / ___/      __(_) /______/ /_
         *   \__ \ | /| / / / __/ ___/ __ \
         *  ___/ / |/ |/ / / /_/ /__/ / / /
         * /____/|__/|__/_/\__/\___/_/ /_/
         *
         */
        final InstructionRenderer<SwitchStmt> switchRenderer = new SwitchJumpRenderer();
        for (BasicBlock vertex : new HashSet<>(cfg.vertices())) {
            new HashSet<>(vertex)
                    .stream()
                    .filter(e -> e instanceof SwitchStmt)
                    .map(e -> (SwitchStmt) e)
                    .forEach(stmt -> switchRenderer.transform(skidfuscator, stmt));
        }

        /*
         *     ______                     __  _
         *    / ____/  __________  ____  / /_(_)___  ____
         *   / __/ | |/_/ ___/ _ \/ __ \/ __/ / __ \/ __ \
         *  / /____>  </ /__/  __/ /_/ / /_/ / /_/ / / / /
         * /_____/_/|_|\___/\___/ .___/\__/_/\____/_/ /_/
         *                     /_/
         */
        for (ExceptionRange<BasicBlock> blockRange : cfg.getRanges()) {
            LinkedHashMap<Integer, BasicBlock> basicBlockMap = new LinkedHashMap<>();
            List<Integer> sortedList = new ArrayList<>();

            // Save current handler
            final BasicBlock basicHandler = blockRange.getHandler();
            final SkidBlock handler = (SkidBlock) blockRange.getHandler();

            // Create new block handle
            final BasicBlock toppleHandler = new SkidBlock(cfg);
            cfg.addVertex(toppleHandler);
            blockRange.setHandler(toppleHandler);

            // Hasher
            final HashTransformer hashTransformer = new BitwiseHashTransformer();

            // Add support for duplicate keys
            final Set<Integer> calledHashes = new HashSet<>();

            // For all block being read
            for (BasicBlock node : blockRange.getNodes()) {
                if (node instanceof FakeBlock)
                    continue;

                // Get their internal seed and add it to the list
                final SkidBlock internal = (SkidBlock) node;

                // Check if key is already generated
                if (calledHashes.contains(internal.getSeed()))
                    continue;
                calledHashes.add(internal.getSeed());

                // Create a new switch block and get it's seeded variant
                final SkidBlock block = new SkidBlock(cfg);
                block.setFlag(SkidBlock.FLAG_PROXY, true);

                cfg.addVertex(block);

                // Add a seed loader for the incoming block and convert it to the handler's
                this.addSeedLoader(
                        block,
                        internal,
                        0,
                        localGetter,
                        localSetter,
                        methodNode.getBlockPredicate(internal),
                        methodNode.getBlockPredicate(handler),
                        "Exception Range " + Arrays.toString(blockRange.getTypes().toArray())
                );

                // Jump to handler
                final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(
                        block,
                        handler
                );
                final UnconditionalJumpStmt proxy = new FakeUnconditionalJumpStmt(handler, edge);
                proxy.setFlag(SkidBlock.FLAG_PROXY, true);
                block.add(proxy);

                cfg.addEdge(edge);

                // Final hashed
                final int hashed = hashTransformer.hash(
                        methodNode.getBlockPredicate(internal),
                        internal,
                        localGetter
                ).getHash();

                // Add to switch
                // TODO: Revert back to hashed
                basicBlockMap.put(hashed, block);
                cfg.addEdge(new SwitchEdge<>(toppleHandler, block, hashed));
                sortedList.add(hashed);

                // Find egde and transform
                cfg.getEdges(node)
                        .stream()
                        .filter(e -> e instanceof TryCatchEdge)
                        .map(e -> (TryCatchEdge<BasicBlock>) e)
                        .filter(e -> e.erange == blockRange)
                        .findFirst()
                        .ifPresent(cfg::removeEdge);

                // Add new edge
                cfg.addEdge(new TryCatchEdge<>(node, blockRange));
            }

            // Haha get fucked
            // Todo     Fix the other shit to re-enable this; this is for the lil shits
            //          (love y'all tho) that are gonna try reversing this
            /*for (int i = 0; i < 10; i++) {
                // Generate random seed + prevent conflict
                final int seed = RandomUtil.nextInt();
                if (sortedList.contains(seed))
                    continue;

                // Add seed to list
                sortedList.add(seed);

                // Create new switch block
                final SkidBlock block = new SkidBlock(cfg);
                cfg.addVertex(block);

                // Get seeded version and add seed loader
                addSeedLoader(

                );
                seededBlock.addSeedLoader(-1, local, seed, RandomUtil.nextInt());
                block.add(new UnconditionalJumpStmt(basicHandler));
                cfg.addEdge(new UnconditionalJumpEdge<>(block, basicHandler));

                basicBlockMap.put(seed, block);
                cfg.addEdge(new SwitchEdge<>(handler.getBlock(), block, seed));
            }*/

            // Hash
            final Expr hash = hashTransformer.hash(toppleHandler, localGetter);

            // Default switch edge
            final BasicBlock defaultBlock = Blocks.exception(cfg, "Error in hash");
            cfg.addEdge(new DefaultSwitchEdge<>(toppleHandler, defaultBlock));

            // Add switch
            final SwitchStmt stmt = new SwitchStmt(hash, basicBlockMap, defaultBlock);
            toppleHandler.add(stmt);

            // Add unconditional jump edge
            cfg.addEdge(new UnconditionalJumpEdge<>(toppleHandler, basicHandler));
        }


        if (methodNode.getName().equals("lambda$null$5") && methodNode.owner.getName().contains("PlayerListener")) {
            System.out.println(cfg.toString());
        }
        cfg.recomputeEdges();
        cfg.verify();

        return;
    }

    /**
     * WIP
     */
    private BasicBlock createRedirector(final BasicBlock block,
                                        final BasicBlock targetBlock,
                                        final PredicateFlowGetter getter,
                                        final PredicateFlowSetter setter,
                                        final int value,
                                        final int target,
                                        final String type
    ) {
        final BasicBlock redirector = new BasicBlock(block.cfg);
        block.cfg.addVertex(redirector);
        return null;
    }


    private void addSeedLoader(final BasicBlock block,
                               final BasicBlock targetBlock,
                               final int index,
                               final PredicateFlowGetter getter,
                               final PredicateFlowSetter local,
                               final int value,
                               final int target,
                               final String type
    ) {
        final Expr load = NumberManager.encrypt(
                target,
                value,
                block,
                getter
        );
        final Stmt set = local.apply(load);

        block.add(
                index < 0 ? block.size() : index,
                set
        );
        if (DEBUG) {
            final BasicBlock exception = Blocks.exception(
                    block.cfg,
                    block.getDisplayName() + " --> "
                            + targetBlock.getDisplayName()
                            + " # Failed to match seed of type "
                            + type
                            + " and value "
                            + target
            );

            final Stmt jumpStmt = new FakeConditionalJumpStmt(
                    getter.get(targetBlock),
                    new ConstantExpr(
                            target,
                            Type.INT_TYPE
                    ),
                    exception,
                    ConditionalJumpStmt.ComparisonType.NE
            );
            block.cfg.addEdge(
                    new ConditionalJumpEdge<>(
                            block,
                            exception,
                            Opcodes.GOTO
                    )
            );
            block.add(
                    index < 0 ? block.size() : index + 1,
                    jumpStmt
            );
        }
    }
}
