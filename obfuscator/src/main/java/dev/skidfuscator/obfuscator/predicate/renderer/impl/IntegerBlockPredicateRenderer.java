package dev.skidfuscator.obfuscator.predicate.renderer.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.clazz.InitClassTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.group.InitGroupTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.PostMethodTransformEvent;
import dev.skidfuscator.obfuscator.number.NumberManager;
import dev.skidfuscator.obfuscator.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.impl.BitwiseHashTransformer;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.*;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidConstantExpr;
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
import org.mapleir.asm.MethodNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.*;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.*;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

public class IntegerBlockPredicateRenderer extends AbstractTransformer {
    public IntegerBlockPredicateRenderer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"GEN3 Flow", children);
    }

    public static final boolean DEBUG = false;

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

                final ClassOpaquePredicate classPredicate = skidMethodNode.isStatic()
                                ? skidMethodNode.getParent().getStaticPredicate()
                                : skidMethodNode.getParent().getPredicate();
                int seed;
                PredicateFlowGetter expr;
                if (skidMethodNode.isClinit() || skidMethodNode.isInit()) {
                    final int randomSeed = RandomUtil.nextInt();
                    seed = randomSeed;
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
            final SkidFieldNode fieldNode = classNode
                    .createField()
                    .access(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT)
                    .name(RandomUtil.randomIsoString(10))
                    .desc("I")
                    .value(clazzInstancePredicate.get())
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
                        final Expr expr = new ConstantExpr(
                                clazzInstancePredicate.get()
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

        final Runnable createDynamicStatic = () -> {
            final SkidFieldNode staticFieldNode = classNode.createField()
                    .access(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)
                    .name(RandomUtil.randomIsoString(10))
                    .desc("I")
                    .value(0)
                    .build();

            final SkidMethodNode clinit = classNode.getClassInit();
            clinit.getEntryBlock().add(
                    0,
                    new FieldStoreStmt(
                        null,
                        new ConstantExpr(clazzStaticPredicate.get(), Type.INT_TYPE),
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
            final boolean addInstance = classNode.getMethods()
                    .stream()
                    .anyMatch(e -> !e.isInit() && !e.isStatic());

            // TODO: Figure out why tf this breaks shit
            if (addInstance) {
                createDynamicInstance.run();
            } else {
                createConstantInstance.run();
            }

            final boolean addStatic = classNode.getMethods()
                    .stream()
                    .anyMatch(e -> !e.isClinit() && e.isStatic());

            if (addStatic) {
                createDynamicStatic.run();
            } else {
                createConstantStatic.run();
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

        for (MethodNode methodNode : skidGroup.getMethodNodeList()) {
            final SkidMethodNode skidMethodNode = (SkidMethodNode) methodNode;

            stackHeight = OpcodeUtil.getArgumentsSizes(methodNode.getDesc());
            if (methodNode.isStatic()) stackHeight -= 1;

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

            final Parameter parameter = new Parameter(methodNode.getDesc());
            parameter.addParameter(Type.INT_TYPE);
            methodNode.node.desc = desc = parameter.getDesc();

            if (local == null) {
                local = skidMethodNode.getCfg().getLocals().get(stackHeight);
            }
        }

        for (SkidInvocation invoker : skidGroup.getInvokers()) {
            assert invoker != null : String.format("Invoker %s is null!", Arrays.toString(skidGroup.getInvokers().toArray()));
            assert invoker.getExpr() != null : String.format("Invoker %s is null!", invoker.getOwner().getDisplayName());

            int index = 0;
            for (Expr argumentExpr : invoker.getExpr().getArgumentExprs()) {
                assert argumentExpr != null : "Argument of index " + index + " is null!";
                index++;
            }

            final Expr[] args = new Expr[invoker.getExpr().getArgumentExprs().length + 1];
            System.arraycopy(
                    invoker.getExpr().getArgumentExprs(),
                    0,
                    args,
                    0,
                    invoker.getExpr().getArgumentExprs().length
            );

            final ConstantExpr constant = new SkidConstantExpr(skidGroup.getPredicate().getPublic());
            args[args.length - 1] = constant;

            invoker.getExpr().setArgumentExprs(args);
            invoker.getExpr().setDesc(desc);
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

        // TODO: Figure out why this happens?
        /*if (getter == null) {
            final SkidGroup group = skidfuscator
                    .getHierarchy()
                    .getGroup(methodNode);

            EventBus.call(new InitGroupTransformEvent(
                    skidfuscator,
                    group
            ));

            getter = group.getPredicate().getGetter();
        }*/
        assert getter != null : "Predicate flow getter is null! Has group not been initialized?";

        PredicateFlowGetter localGetterT = methodNode.getFlowPredicate().getGetter();
        PredicateFlowSetter localSetterT = methodNode.getFlowPredicate().getSetter();

        // TODO: Figure out why this is happening too
        /*if (localGetterT == null || localSetterT == null) {
            EventBus.call(new InitMethodTransformEvent(
                    skidfuscator,
                    methodNode
            ));

            localGetterT = methodNode.getFlowPredicate().getGetter();
            localSetterT = methodNode.getFlowPredicate().getSetter();
        }*/
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
                        final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(
                                e.src(),
                                e.dst()
                        );
                        cfg.removeEdge(e);
                        cfg.addEdge(edge);
                        e.src().add(new UnconditionalJumpStmt(e.dst(), edge));
                    });
        }

        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            new HashSet<>(block)
                    .stream()
                    .filter(e -> e instanceof UnconditionalJumpStmt && !(e instanceof FakeUnconditionalJumpStmt))
                    .map(e -> (UnconditionalJumpStmt) e)
                    .forEach(stmt -> {
                        final int index = block.indexOf(stmt);
                        final SkidBlock seededBlock = (SkidBlock) block;
                        final SkidBlock targetSeededBlock = (SkidBlock) stmt.getTarget();
                        this.addSeedLoader(
                                seededBlock,
                                targetSeededBlock,
                                index,
                                localGetter,
                                localSetter,
                                methodNode.getBlockPredicate(seededBlock),
                                methodNode.getBlockPredicate(targetSeededBlock)
                        );

                        if (DEBUG) {
                            final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
                            block.add(index, new SkidCopyVarStmt(
                                            new VarExpr(local1, Type.getType(String.class)),
                                            new ConstantExpr(
                                                    block.getDisplayName()
                                                            + " -> " + targetSeededBlock.getDisplayName()
                                                            + " : c-loc - uncond : "
                                                            + methodNode.getBlockPredicate(targetSeededBlock)
                                            )
                                    )
                            );
                        }
                    });
        }

        /*
         *    ______                ___ __  _                   __
         *   / ____/___  ____  ____/ (_) /_(_)___  ____  ____ _/ /
         *  / /   / __ \/ __ \/ __  / / __/ / __ \/ __ \/ __ `/ /
         * / /___/ /_/ / / / / /_/ / / /_/ / /_/ / / / / /_/ / /
         * \____/\____/_/ /_/\__,_/_/\__/_/\____/_/ /_/\__,_/_/
         *
         */

        for (BasicBlock block : new HashSet<>(cfg.vertices())) {
            new HashSet<>(block)
                    .stream()
                    .filter(e -> e instanceof ConditionalJumpStmt && !(e instanceof FakeConditionalJumpStmt))
                    .map(e -> (ConditionalJumpStmt) e)
                    .forEach(stmt -> {
                        // TODO: Not necessary for now
                        /*final ConditionalJumpEdge<BasicBlock> edge = block.cfg.getEdges(block).stream()
                                .filter(e -> e instanceof ConditionalJumpEdge && !(e instanceof FakeConditionalJumpEdge))
                                .map(e -> (ConditionalJumpEdge<BasicBlock>) e)
                                .filter(e -> e.dst().equals(stmt.getTrueSuccessor()))
                                .findFirst()
                                .orElse(null);

                        block.cfg.removeEdge(edge);*/

                        final SkidBlock seededBlock = (SkidBlock) block;
                        final BasicBlock target = stmt.getTrueSuccessor();
                        final SkidBlock targetSeeded = (SkidBlock) target;

                        // Add jump and seed
                        final SkidBlock basicBlock = new SkidBlock(block.cfg);

                        methodNode.getFlowPredicate()
                                .set(basicBlock, methodNode.getBlockPredicate(targetSeeded));

                        this.addSeedLoader(
                                basicBlock,
                                targetSeeded,
                                0,
                                localGetter,
                                localSetter,
                                methodNode.getBlockPredicate(seededBlock),
                                methodNode.getBlockPredicate(targetSeeded)
                        );
                        final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(basicBlock, target);
                        basicBlock.add(new UnconditionalJumpStmt(target, edge));

                        // Add edge
                        basicBlock.cfg.addVertex(basicBlock);
                        basicBlock.cfg.addEdge(edge);

                        // Replace successor
                        stmt.setTrueSuccessor(basicBlock);
                        basicBlock.cfg.addEdge(new ConditionalJumpEdge<>(block, basicBlock, Opcodes.IF_ICMPEQ));

                        if (DEBUG) {
                            final Local local1 = basicBlock.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
                            basicBlock.add(
                                    1,
                                    new SkidCopyVarStmt(
                                            new VarExpr(local1, Type.getType(String.class)),
                                            new ConstantExpr(
                                                    block.getDisplayName()
                                                    + " -> " + targetSeeded.getDisplayName()
                                                    + " : c-loc - cond : "
                                                    + methodNode.getBlockPredicate(targetSeeded)
                                            )
                                    )
                            );
                        }
                    });
        }


        /*for (BasicBlock vertex : new HashSet<>(cfg.vertices())) {
            if (vertex instanceof FakeBlock)
                continue;

            new HashSet<>(cfg.getEdges(vertex))
                    .stream()
                    .filter(e -> e instanceof ImmediateEdge)
                    .forEach(e -> {
                        final SkidBlock seededBlock = (SkidBlock) e.src();
                        final SkidBlock targetSeededBlock = (SkidBlock) e.dst();
                        this.addSeedLoader(seededBlock,
                                -1,
                                localGetter,
                                localSetter,
                                methodNode.getBlockPredicate(seededBlock),
                                methodNode.getBlockPredicate(targetSeededBlock)
                        );

                        if (DEBUG) {
                            final Local local1 = vertex.cfg.getLocals().get(vertex.cfg.getLocals().getMaxLocals() + 2);
                            vertex.add(vertex.size(), new CopyVarStmt(new VarExpr(local1, Type.getType(String.class)),
                                    new ConstantExpr(vertex.getDisplayName() +" : c-loc - immediate : " + methodNode.getBlockPredicate((SkidBlock) vertex))));
                        }
                    });
        */

        /*
         *    _____         _ __       __
         *   / ___/      __(_) /______/ /_
         *   \__ \ | /| / / / __/ ___/ __ \
         *  ___/ / |/ |/ / / /_/ /__/ / / /
         * /____/|__/|__/_/\__/\___/_/ /_/
         *
         */

        for (BasicBlock vertex : new HashSet<>(cfg.vertices())) {
            new HashSet<>(vertex)
                    .stream()
                    .filter(e -> e instanceof SwitchStmt)
                    .map(e -> (SwitchStmt) e)
                    .forEach(stmt -> {
                        final SkidBlock seededBlock = (SkidBlock) vertex;

                        for (Map.Entry<Integer, BasicBlock> entry : stmt.getTargets().entrySet()) {
                            final int seed = entry.getKey();
                            final BasicBlock value = entry.getValue();
                            if (value == stmt.getDefaultTarget())
                                continue;

                            final SkidBlock target = (SkidBlock) value;
                            // Add jump and seed
                            final SkidBlock basicBlock = new SkidBlock(value.cfg);

                            methodNode.getFlowPredicate()
                                    .set(basicBlock, methodNode.getBlockPredicate(target));

                            this.addSeedLoader(
                                    basicBlock,
                                    target,
                                    0,
                                    localGetter,
                                    localSetter,
                                    methodNode.getBlockPredicate(seededBlock),
                                    methodNode.getBlockPredicate(target)
                            );
                            final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(basicBlock, target);
                            basicBlock.add(new UnconditionalJumpStmt(target, edge));

                            // Add edge
                            basicBlock.cfg.addVertex(basicBlock);
                            basicBlock.cfg.addEdge(edge);

                            // Replace successor
                            stmt.getTargets().replace(seed, basicBlock);
                            basicBlock.cfg.addEdge(new SwitchEdge<>(seededBlock, basicBlock, stmt.getOpcode()));

                            if (DEBUG) {
                                final Local local1 = basicBlock.cfg.getLocals().get(seededBlock.cfg.getLocals().getMaxLocals() + 2);
                                basicBlock.add(
                                        1,
                                        new SkidCopyVarStmt(
                                                new VarExpr(local1, Type.getType(String.class)),
                                                new ConstantExpr(
                                                        seededBlock.getDisplayName()
                                                                + " -> " + target.getDisplayName()
                                                                + " : c-loc - switch : "
                                                                + methodNode.getBlockPredicate(target)
                                                )
                                        )
                                );
                            }
                        }

                        if (stmt.getDefaultTarget() == null || stmt.getDefaultTarget() == vertex)
                            return;

                        final SkidBlock target = (SkidBlock) stmt.getDefaultTarget();
                        // Add jump and seed
                        final SkidBlock basicBlock = new SkidBlock(target.cfg);

                        methodNode.getFlowPredicate()
                                .set(basicBlock, methodNode.getBlockPredicate(target));

                        this.addSeedLoader(
                                basicBlock,
                                target,
                                0,
                                localGetter,
                                localSetter,
                                methodNode.getBlockPredicate(seededBlock),
                                methodNode.getBlockPredicate(target)
                        );
                        final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(basicBlock, target);
                        basicBlock.add(new UnconditionalJumpStmt(target, edge));

                        // Add edge
                        basicBlock.cfg.addVertex(basicBlock);
                        basicBlock.cfg.addEdge(edge);

                        // Replace successor
                        stmt.setDefaultTarget(basicBlock);
                        basicBlock.cfg.addEdge(new SwitchEdge<>(seededBlock, basicBlock, stmt.getOpcode()));

                        if (DEBUG) {
                            final Local local1 = basicBlock.cfg.getLocals().get(seededBlock.cfg.getLocals().getMaxLocals() + 2);
                            basicBlock.add(
                                    1,
                                    new SkidCopyVarStmt(
                                            new VarExpr(local1, Type.getType(String.class)),
                                            new ConstantExpr(
                                                    seededBlock.getDisplayName()
                                                            + " -> " + target.getDisplayName()
                                                            + " : c-loc - switch : "
                                                            + methodNode.getBlockPredicate(target)
                                            )
                                    )
                            );
                        }
                    });
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

            // For all block being read
            for (BasicBlock node : blockRange.getNodes()) {
                if (node instanceof FakeBlock)
                    continue;

                // Get their internal seed and add it to the list
                final SkidBlock internal = (SkidBlock) node;

                // Create a new switch block and get it's seeded variant
                final SkidBlock block = new SkidBlock(cfg);
                cfg.addVertex(block);

                // Add a seed loader for the incoming block and convert it to the handler's
                this.addSeedLoader(
                        block,
                        internal,
                        0,
                        localGetter,
                        localSetter,
                        methodNode.getBlockPredicate(internal),
                        methodNode.getBlockPredicate(handler)
                );

                // Jump to handler
                final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(
                        block,
                        handler
                );
                block.add(new FakeUnconditionalJumpStmt(handler, edge));
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
                final BasicBlock block = new BasicBlock(cfg);
                cfg.addVertex(block);

                // Get seeded version and add seed loader
                final SkidBlock seededBlock = getBlock(block);
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

        return;
    }


    private void addSeedLoader(final BasicBlock block, final BasicBlock targetBlock, final int index, final PredicateFlowGetter getter, final PredicateFlowSetter local, final int value, final int target) {
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
                    "Failed to match seed of value " + target
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
