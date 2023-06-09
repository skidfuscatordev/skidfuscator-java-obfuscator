package dev.skidfuscator.obfuscator.predicate.renderer;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.clazz.InitClassTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.group.InitGroupTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.PostMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.skid.InitSkidTransformEvent;
import dev.skidfuscator.obfuscator.hierarchy.matching.ClassMethodHash;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obfuscator.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obfuscator.number.hash.impl.LegacyHashTransformer;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.ConditionalJumpRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.ExceptionRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.SwitchJumpRenderer;
import dev.skidfuscator.obfuscator.predicate.renderer.impl.UnconditionalJumpRenderer;
import dev.skidfuscator.obfuscator.skidasm.*;
import dev.skidfuscator.obfuscator.skidasm.builder.SkidClassNodeBuilder;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidIntegerParseStaticInvocationExpr;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeConditionalJumpStmt;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeUnconditionalJumpStmt;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidSwitchStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import dev.skidfuscator.obfuscator.util.OpcodeUtil;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
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

    @Listen
    void handle(final InitSkidTransformEvent event) {
        final SkidClassNode factory = new SkidClassNodeBuilder(skidfuscator)
                .name("skid/Factory")
                .access(Opcodes.ACC_PUBLIC)
                .phantom(true)
                .virtual(true)
                .build();

        skidfuscator.setFactoryNode(factory);
        /*skidfuscator.getClassRemapper().add(
                Type.getObjectType(factory.getName()).getInternalName(),
                RandomUtil.randomAlphabeticalString(16)
        );*/
        skidfuscator
                .getJarContents()
                .getClassContents()
                .add(
                        new JarClassData(
                                "skid/Factory.class",
                                factory.toByteArray(),
                                factory
                        )
                );

        skidfuscator.setLegacyHasher(new LegacyHashTransformer(skidfuscator));
        skidfuscator.setBitwiseHasher(new BitwiseHashTransformer(skidfuscator));
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
                                : skidMethodNode.getParent().getClassPredicate();
                int seed;
                PredicateFlowGetter expr;
                if (skidMethodNode.isClinit() || skidMethodNode.isInit()) {
                    final int randomSeed = skidClassNode.getRandomInt();
                    seed = randomSeed;

                    expr = new PredicateFlowGetter() {
                        @Override
                        public Expr get(BasicBlock vertex) {
                            return new SkidIntegerParseStaticInvocationExpr(randomSeed);
                        }
                    };
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

        final ClassOpaquePredicate clazzInstancePredicate = classNode.getClassPredicate();
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
                    return new PopStmt(expr);
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
                    return new PopStmt(expr);
                    //throw new IllegalStateException("Cannot set value for a constant getter");
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

            createDynamicStatic2.run();

            final boolean addInstance = classNode.getMethods()
                    .stream()
                    .anyMatch(e -> !e.isInit() && !e.isStatic());

            // TODO: Figure out why tf this breaks shit
            createDynamicInstance.run();
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

            if ((methodNode.node.access & Opcodes.ACC_VARARGS) != 0) {
                methodNode.node.access &= ~Opcodes.ACC_VARARGS;
            }

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

                for (Expr arg : args) {
                    assert arg != null : "Invocation now is null? " + invoker.asExpr();
                }

                invoker.getExpr().setArgumentExprs(args);
                //System.out.println(invoker.asExpr());
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
                    final Parameter parameter = new Parameter(invoker.getExpr().getDesc());
                    parameter.insertParameter(Type.INT_TYPE, indexGroup);
                    //invoker.getExpr().setDesc(parameter.getDesc());
                }
            }
        }

        final int finalStackHeight = stackHeight;
        skidGroup.setDesc(desc);
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
                                "Immediate"
                        );
                    });
        }

        final InstructionRenderer<UnconditionalJumpStmt> unconditionalRenderer = new UnconditionalJumpRenderer();
        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            new HashSet<>(block)
                    .stream()
                    .filter(e -> e instanceof UnconditionalJumpStmt && !(e instanceof FakeUnconditionalJumpStmt))
                    .map(e -> (UnconditionalJumpStmt) e)
                    .forEach(stmt -> unconditionalRenderer.transform(skidfuscator, cfg, stmt));
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
                    .forEach(stmt -> conditionRenderer.transform(skidfuscator, cfg, stmt));
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
                    .filter(e -> e instanceof SkidSwitchStmt)
                    .map(e -> (SwitchStmt) e)
                    .forEach(stmt -> switchRenderer.transform(skidfuscator, cfg, stmt));
        }

        /*
         *     ______                     __  _
         *    / ____/  __________  ____  / /_(_)___  ____
         *   / __/ | |/_/ ___/ _ \/ __ \/ __/ / __ \/ __ \
         *  / /____>  </ /__/  __/ /_/ / /_/ / /_/ / / / /
         * /_____/_/|_|\___/\___/ .___/\__/_/\____/_/ /_/
         *                     /_/
         */
        final ExceptionRenderer exceptionRenderer = new ExceptionRenderer();
        for (ExceptionRange<BasicBlock> blockRange : cfg.getRanges()) {
            exceptionRenderer.transform(skidfuscator, cfg, blockRange);
        }

        if (methodNode.getName().equals("lambda$null$5") && methodNode.owner.getName().contains("PlayerListener")) {
            System.out.println(cfg.toString());
        }
        cfg.recomputeEdges();
        cfg.verify();

        return;
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
