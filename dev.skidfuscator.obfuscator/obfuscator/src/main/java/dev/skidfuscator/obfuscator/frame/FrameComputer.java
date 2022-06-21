package dev.skidfuscator.obfuscator.frame;

import com.google.common.collect.Streams;
import com.google.errorprone.annotations.Var;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidExpressionPool;
import dev.skidfuscator.obfuscator.skidasm.SkidTypeStack;
import dev.skidfuscator.obfuscator.util.TypeUtil;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
import org.mapleir.asm.ClassNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.TryCatchEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.ExpressionPool;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.TypeStack;
import org.mapleir.ir.code.expr.*;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.ThrowStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.utils.CFGExporterUtils;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.BooleanProperty;
import org.mapleir.propertyframework.util.PropertyHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FrameComputer {
    private final Skidfuscator skidfuscator;
    public static final int FLAG_CONFLICT = 0x1;

    public FrameComputer(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }

    public void compute(final ControlFlowGraph cfg) {
        if (cfg.getEntries().size() != 1)
            throw new IllegalStateException("CFG doesn't have exactly 1 entry");
        final BasicBlock entry = cfg.getEntries().iterator().next();
        final FrameGraph frameGraph = new FrameGraph();

        final Map<BasicBlock, FrameNode> frameMap = new HashMap<>();

        /*
         * Create a hot cache for a frame node for every
         * frame assigned per block, then add it to the
         * flame flow graph cuz it's useful for debugging.
         */
        for (BasicBlock vertex : cfg.vertices()) {
            final SkidExpressionPool pool = new SkidExpressionPool(
                    new Type[cfg.getLocals().getMaxLocals() + 2],
                    skidfuscator
            );

            final FrameNode frameNode = new FrameNode(vertex, pool);
            frameMap.put(
                    vertex,
                    frameNode
            );
            frameGraph.addVertex(frameNode);
        }

        final FrameNode entryFrame = frameMap.get(entry);

        /*
         * This is just shit for iterating.
         */
        final Set<BasicBlock> visited = new HashSet<>();
        final Stack<BasicBlock> bucket = new Stack<>();

        /*
         * FRAME LOCALS
         */
        bucket.add(entry);

        /*
         * At the beginning, lets assume every variable has
         * no definition
         */
        entryFrame.fill(TypeUtil.UNDEFINED_TYPE);

        int index = 0;
        int protectedIndex = 0;

        /*
         * If the method is a non static, then it has a def
         * of "this." which is itself
         */
        if (!cfg.getMethodNode().isStatic()) {
            entryFrame.set(index, Type.getType("L" + cfg.getMethodNode().owner.getName() + ";"));
            protectedIndex = index;
            index++;
        }

        /*
         * Every method parameter reserves either:
         * - 2 slots of locals for double and long types
         * - 1 slot of locals for the rest
         */
        final Parameter parameter = new Parameter(cfg.getDesc());
        for (int i = 0; i < parameter.getArgs().size(); i++) {
            Type type = parameter.getArg(i);

            if (type == Type.BOOLEAN_TYPE
                    || type == Type.BYTE_TYPE
                    || type == Type.CHAR_TYPE
                    || type == Type.SHORT_TYPE) {
                type = Type.INT_TYPE;
            }

            entryFrame.set(index, type);
            protectedIndex = index;
            index++;
        }

        /*cfg.allExprStream()
                .filter(InvocationExpr.class::isInstance)
                .map(InvocationExpr.class::cast)
                .forEach(e -> {
                    final Type[] types = Type.getArgumentTypes(e.getDesc());
                    final Expr[] args = e instanceof DynamicInvocationExpr
                            ? e.getArgumentExprs()
                            : e.getPrintedArgs();

                    for (int i = 0; i < args.length; i++) {
                        final Type descType = types[i];
                        final Expr expr = args[i];

                        if (expr instanceof VarExpr) {
                            final VarExpr varExpr = (VarExpr) expr;
                            varExpr.setType(descType);
                        }
                    }
                });*/

        // TODO:
        while (!bucket.isEmpty()) {
            /* Visit the top of the stack */
            final BasicBlock popped = bucket.pop();
            final FrameNode poppedFrame = frameMap.get(popped);
            visited.add(popped);

            final Set<BasicBlock> next = new HashSet<>();

            SkidExpressionPool currentPool = new SkidExpressionPool(
                    poppedFrame.getPool(),
                    skidfuscator
            );

            SkidExpressionPool highestPoolNoRandoAssExpression = currentPool;
            boolean yeahNo = false;

            for (Stmt stmt : popped) {
                /*
                 * We're currently iterating the exception
                 * block. We're trying to see how much of
                 * scope we can say "ok this is fine
                 */
                if (!yeahNo) {
                    if (stmt instanceof ThrowStmt) {
                        yeahNo = true;
                    } else {
                        /*for (Expr expr : stmt.enumerateOnlyChildren()) {
                            final boolean fuck = expr instanceof InvocationExpr
                                    || expr instanceof FieldLoadExpr;

                            if (fuck) {
                                yeahNo = true;
                                break;
                            }
                        }*/
                        if (Streams.stream(stmt.enumerateOnlyChildren())
                                .noneMatch(CaughtExceptionExpr.class::isInstance)) {
                            yeahNo = true;
                        }
                    }
                }

                /*
                 * When we'll proceed with the jump, we will be
                 * using the details processed in this. If we
                 * have an if conditon which checks if x or y
                 * is null, we can update it so that it ONLY
                 * updates anything after the jump, not any
                 * other statement in the block.
                 */
                final SkidExpressionPool targetCurrentPool = new SkidExpressionPool(
                        currentPool,
                        skidfuscator
                );

                if (stmt instanceof CopyVarStmt) {
                    final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;
                    final Expr expr = copyVarStmt.getExpression();

                    if(copyVarStmt.getExpression() instanceof VarExpr) {
                        if(((VarExpr) copyVarStmt.getExpression()).getLocal()
                                == copyVarStmt.getVariable().getLocal()) {
                            continue;
                        }
                    }

                    Type type = copyVarStmt.getType();

                    if (type == Type.BOOLEAN_TYPE
                            || type == Type.BYTE_TYPE
                            || type == Type.CHAR_TYPE
                            || type == Type.SHORT_TYPE) {
                        type = Type.INT_TYPE;
                    }

                    if (expr instanceof ConstantExpr) {
                        final ConstantExpr constantExpr = (ConstantExpr) expr;
                        if (constantExpr.getConstant() == null) {
                            type = TypeUtil.NULL_TYPE;
                        }
                    }

                    currentPool.set(copyVarStmt.getIndex(), type);
                } else {
                    final Map<BasicBlock, Stmt> vars = new HashMap<>();

                    if (stmt instanceof SwitchStmt) {
                        final SwitchStmt switchStmt = (SwitchStmt) stmt;

                        for (BasicBlock value : switchStmt.getTargets().values()) {
                            vars.put(value, stmt);
                        }

                        vars.put(switchStmt.getDefaultTarget(), switchStmt);
                    } else if (stmt instanceof ConditionalJumpStmt) {
                        final ConditionalJumpStmt conditionalJumpStmt = (ConditionalJumpStmt) stmt;

                        final Expr left = conditionalJumpStmt.getLeft();
                        final Expr right = conditionalJumpStmt.getRight();

                        vars.put(conditionalJumpStmt.getTrueSuccessor(), stmt);

                        final boolean isLeft = left instanceof VarExpr
                                && right.getType().equals(TypeUtil.NULL_TYPE);

                        final boolean isRight = right instanceof VarExpr
                                && left.getType().equals(TypeUtil.NULL_TYPE);

                        if (isLeft || isRight) {
                            final VarExpr varExpr = (VarExpr) (isLeft ? left : right);
                            switch (conditionalJumpStmt.getComparisonType()) {
                                case EQ: {
                                    currentPool = new SkidExpressionPool(
                                            currentPool,
                                            skidfuscator
                                    );
                                    currentPool.addExclusion(TypeUtil.NULL_TYPE);
                                    targetCurrentPool.set(varExpr.getIndex(), TypeUtil.NULL_TYPE);
                                    break;
                                }

                                case NE: {
                                    currentPool = new SkidExpressionPool(
                                            currentPool,
                                            skidfuscator
                                    );
                                    currentPool.set(varExpr.getIndex(), TypeUtil.NULL_TYPE);
                                    targetCurrentPool.addExclusion(TypeUtil.NULL_TYPE);
                                    break;
                                }
                            }

                            conditionalJumpStmt.setFlag(FLAG_CONFLICT, true);
                        }

                    } else if (stmt instanceof UnconditionalJumpStmt) {
                        final UnconditionalJumpStmt unconditionalJumpStmt = (UnconditionalJumpStmt) stmt;
                        vars.put(unconditionalJumpStmt.getTarget(), stmt);
                    }

                    next.addAll(vars.keySet());

                    vars.forEach((target, _stmt) -> {
                        final FrameNode targetFrame = frameMap.get(target);

                        frameGraph.addEdge(new FrameEdge(
                                poppedFrame,
                                targetFrame,
                                targetCurrentPool
                        ));

                        targetFrame.getPool().addParent(targetCurrentPool);
                    });

                    currentPool = new SkidExpressionPool(
                            currentPool,
                            skidfuscator
                    );
                }

                if (!yeahNo) {
                    highestPoolNoRandoAssExpression = currentPool;
                }
            }

            final ExpressionPool finalHighestPoolNoRandoAssExpression = cfg
                    .getReverseEdges(popped)
                    .stream()
                    .filter(TryCatchEdge.class::isInstance)
                    .map(e -> (TryCatchEdge<BasicBlock>) e)
                    .anyMatch(e -> e.erange.getHandler().equals(popped))
                        ? highestPoolNoRandoAssExpression
                        : poppedFrame.getPool();

            cfg.getSuccessors(new Predicate<FlowEdge<BasicBlock>>() {
                @Override
                public boolean test(FlowEdge<BasicBlock> basicBlockFlowEdge) {
                    return basicBlockFlowEdge instanceof TryCatchEdge
                            && ((TryCatchEdge<BasicBlock>) basicBlockFlowEdge)
                            .erange
                            .getNodes()
                            .contains(popped);
                }
            }, popped).forEach(e -> {
                final BasicBlock value = e.dst();
                final FrameNode targetFrame = frameMap.get(value);

                frameGraph.addEdge(new FrameEdge(
                        poppedFrame,
                        targetFrame,
                        finalHighestPoolNoRandoAssExpression
                ));

                // TODO:    Check if we should instead take the first
                //          dominator index of the graph
                targetFrame.getPool().addParent(finalHighestPoolNoRandoAssExpression);

                next.add(value);
            });

            final BasicBlock value = cfg.getImmediate(popped);
            if (value != null) {
                final FrameNode targetFrame = frameMap.get(value);
                targetFrame.getPool().addParent(currentPool);

                frameGraph.addEdge(new FrameEdge(
                        poppedFrame,
                        targetFrame,
                        currentPool
                ));

                next.add(value);
            }

            cfg.getEdges(popped)
                    .stream()
                    .filter(e -> !next.contains(e.dst()))
                    .forEach(e -> {
                        System.out.println("Failed " + e + " (Despite "
                                + Arrays.toString(next.stream().map(BasicBlock::getDisplayName).toArray())
                                + ")"
                        );
                    });
            //System.out.println("-----");

            /* Add all the successor nodes */
            /* Add it to the stack to be iterated again */
            next.stream()
                    .filter(e -> !visited.contains(e))
                    .forEach(bucket::add);
        }

        visited.clear();
        bucket.add(entry);
        while (!bucket.isEmpty()) {
            final BasicBlock popped = bucket.pop();
            final FrameNode poppedFrame = frameMap.get(popped);
            visited.add(popped);

            final Set<BasicBlock> next = new HashSet<>();
            Set<Integer> visitedLocal = new HashSet<>();

            for (Stmt stmt : popped) {
                for (Expr e : stmt.enumerateOnlyChildren()) {
                    /*
                     * Iterate through every var expression. If the
                     * variable is instantiated as an object, then
                     * we can define what kind of object it is (cuz
                     * sometimes it can't figure it out. Yep...)
                     *
                     * If the variable is null though, we shouldn't
                     * be changing it.
                     *
                     * And if it is void (TOP), we
                     * should override it as it means we have finally
                     * computed it.
                     */
                    if (e instanceof VarExpr) {
                        final VarExpr expr = (VarExpr) e;
                        final int localIndex = expr.getIndex();

                        if (visitedLocal.contains(localIndex))
                            continue;

                        if (expr.getParent().isFlagSet(FLAG_CONFLICT))
                            continue;

                        final Type currentType = poppedFrame.compute(localIndex);
                        final Type desiredType = expr.getType();
                        if (currentType.equals(TypeUtil.OBJECT_TYPE)) {
                            if (desiredType.getSort() == Type.OBJECT
                                    && !desiredType.equals(TypeUtil.NULL_TYPE)) {
                                poppedFrame.set(localIndex, desiredType);
                            }
                        } else if (currentType.equals(Type.VOID_TYPE)
                                || currentType.equals(TypeUtil.UNDEFINED_TYPE)) {
                            poppedFrame.set(localIndex, desiredType);
                        } else if (currentType.equals(TypeUtil.NULL_TYPE)) {
                            continue;
                        }

                    } else if (e instanceof InstanceofExpr) {
                        final InstanceofExpr expr = (InstanceofExpr) e;

                        if (!(expr.getExpression() instanceof VarExpr)) {
                            continue;
                        }

                        final VarExpr varExpr = (VarExpr) expr.getExpression();
                        final int localIndex = varExpr.getIndex();

                        /* Already defined before. We can skip... */
                        if (visitedLocal.contains(localIndex))
                            continue;

                        /*
                         * Here we basically take the computed type
                         * we try to get, then we merge it with the
                         * instanceof type. This ensures we either
                         * get a hierarchy either get a TOP since
                         * the type could be not computable.
                         */
                        final Type currentType = poppedFrame.compute(localIndex);
                        poppedFrame.set(
                                localIndex,
                                TypeUtil.mergeTypes(skidfuscator,
                                        currentType,
                                        expr.getCheckType()
                                )
                        );
                    }
                }

                if (stmt instanceof CopyVarStmt) {
                    final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;
                    final Expr expr = copyVarStmt.getExpression();
                    final VarExpr varExpr = copyVarStmt.getVariable();

                    /*if (!visitedLocal.contains(varExpr.getIndex())) {
                        System.out.println("Overriding " + varExpr.getIndex() + " with "
                                + varExpr.getType() + " previously was " + poppedFrame.compute(varExpr.getIndex()));
                        poppedFrame.set(varExpr.getIndex(), varExpr.getType());
                    }*/
                    /*
                     * Special override of the parent frame as caught
                     * expressions are essentially implicit, whereas
                     * mapleir makes them explicit cuz exceptions are
                     * grrr
                     */
                    /*if (expr instanceof CaughtExceptionExpr) {
                        popped.getPool().set(copyVarStmt.getIndex(), Type.VOID_TYPE);
                    } else {
                        final Type mergedType = TypeUtil.mergeTypes(
                                skidfuscator,
                                popped.getPool().get(copyVarStmt.getIndex()),
                                copyVarStmt.getType()
                        );

                        popped.getPool().set(copyVarStmt.getIndex(), mergedType);
                    }*/

                    visitedLocal.add(copyVarStmt.getIndex());
                } else {
                    final Map<BasicBlock, Stmt> vars = new HashMap<>();

                    if (stmt instanceof SwitchStmt) {
                        final SwitchStmt switchStmt = (SwitchStmt) stmt;

                        for (BasicBlock value : switchStmt.getTargets().values()) {
                            vars.put(value, stmt);
                        }

                        vars.put(switchStmt.getDefaultTarget(), switchStmt);
                    } else if (stmt instanceof ConditionalJumpStmt) {
                        final ConditionalJumpStmt conditionalJumpStmt = (ConditionalJumpStmt) stmt;
                        vars.put(conditionalJumpStmt.getTrueSuccessor(), stmt);

                    } else if (stmt instanceof UnconditionalJumpStmt) {
                        final UnconditionalJumpStmt unconditionalJumpStmt = (UnconditionalJumpStmt) stmt;
                        vars.put(unconditionalJumpStmt.getTarget(), stmt);
                    }

                    next.addAll(vars.keySet());
                }
            }

            cfg.getSuccessors(new Predicate<FlowEdge<BasicBlock>>() {
                @Override
                public boolean test(FlowEdge<BasicBlock> basicBlockFlowEdge) {
                    return basicBlockFlowEdge instanceof TryCatchEdge
                            && ((TryCatchEdge) basicBlockFlowEdge)
                            .erange
                            .getNodes()
                            .contains(popped);
                }
            }, popped).forEach(e -> {
                next.add(e.dst());
            });

            final BasicBlock value = cfg.getImmediate(popped);
            if (value != null) {
                next.add(value);
            }

            /* Add all the successor nodes */
            /* Add it to the stack to be iterated again */
            next.stream()
                    .filter(e -> !visited.contains(e))
                    .forEach(bucket::add);
        }

        for (BasicBlock vertex : cfg.vertices()) {
            for (Stmt stmt : vertex) {
                if (stmt instanceof CopyVarStmt) {
                    final CopyVarStmt copyVarStmt = (CopyVarStmt) stmt;
                    final Expr expr = copyVarStmt.getExpression();

                    if (cfg.getName().equals("main") && copyVarStmt.getIndex() == 33) {
                        System.out.println("FOUND " + copyVarStmt + " at  " + expr + " of " + copyVarStmt.getType());
                    }
                    /*
                     * Special override of the parent frame as caught
                     * expressions are essentially implicit, whereas
                     * mapleir makes them explicit cuz exceptions are
                     * grrr
                     */
                    if (expr instanceof CaughtExceptionExpr) {
                        frameMap.get(vertex).set(copyVarStmt.getIndex(), Type.VOID_TYPE);
                    } else {
                        /*final Type mergedType = TypeUtil.mergeTypes(
                                skidfuscator,
                                frameMap.get(vertex).compute(copyVarStmt.getIndex()),
                                copyVarStmt.getType()
                        );*/

                        //frameMap.get(vertex).set(copyVarStmt.getIndex(), copyVarStmt.getType());
                    }
                }
            }
        }

        for (BasicBlock vertex : cfg.verticesInOrder()) {
            final FrameNode frameNode = frameMap.get(vertex);

            final Type[] types = new Type[frameNode.getPool().getTypes().length];
            for (int i = 0; i < types.length; i++) {
                types[i] = frameNode.compute(i);

                if (types[i] == null) {
                    throw new IllegalStateException(
                            "Failed to match frame type on method "
                                    + "Frame " + frameNode.getDisplayName() + " { \n  "
                                    + cfg.getMethodNode().getOwner()
                                    + "#"
                                    + cfg.getMethodNode().getDisplayName()
                                    + "\n  "
                                    + " (index: " + i
                                    + " static: " + cfg.getMethodNode().isStatic()
                                    + " desc: " + cfg.getDesc() + ")"
                                    + "\n  Current: " + Arrays.toString(types)
                                    + "\n  Graph Parents: " + frameGraph.getReverseEdges(frameNode).stream().map(e -> e.src().getDisplayName()).collect(Collectors.joining(", "))
                                    + "\n  Scope: \n    >>>  "
                                    + frameMap.values()
                                    .stream()
                                    .map(c -> c.getDisplayName() + " --> " + Arrays.toString(c.getPool().getTypes()))
                                    .collect(Collectors.joining("\n    >>>  "))
                                    + "\n}\n\n"
                                    + "\n  Edges: \n"
                                    + cfg.makeDotGraph().toString()
                                    + "\n--------\n"
                    );
                }
            }

            /*System.out.println(
                    "Frame " + frameNode.getDisplayName() + " { \n  "
                            + cfg.getMethodNode().getOwner()
                            + "#"
                            + cfg.getMethodNode().getDisplayName()
                            + "\n  "
                            + " (static: " + cfg.getMethodNode().isStatic()
                            + " desc: " + cfg.getDesc() + ")"
                            + "\n  Current: " + Arrays.toString(types)
                            + "\n  Graph Parents: " + frameGraph.getReverseEdges(frameNode).stream().map(e -> e.src().getDisplayName()).collect(Collectors.joining(", "))
                            + "\n}\n\n"
            );*/

            final SkidExpressionPool expressionPool = new SkidExpressionPool(
                    types,
                    skidfuscator
            );

            vertex.setPool(expressionPool);
        }

        if (cfg.getName().equals("exportLog")) {
            IPropertyDictionary dict = PropertyHelper.createDictionary();
            dict.put(new BooleanProperty(CFGExporterUtils.OPT_EDGES, true));
            dict.put(new BooleanProperty(CFGExporterUtils.OPT_STMTS, true));
            System.out.println(FrameExporterUtils.makeDotGraph(frameGraph, dict).toString());
        }

        /*
         * FRAME STACK
         */
        for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
            final TypeStack expressionStack = new SkidTypeStack(
                    cfg.getLocals().getMaxStack(),
                    skidfuscator
            );
            final Type type = getRangeType(range);

            expressionStack.push(type);
            range.getHandler().setStack(expressionStack);

            visited.clear();
            bucket.add(range.getHandler());

            while (!bucket.isEmpty()) {
                final BasicBlock popped = bucket.pop();
                visited.add(popped);

                if (popped.stream()
                        .flatMap(e -> Streams.stream(e.enumerateOnlyChildren()))
                        .filter(CaughtExceptionExpr.class::isInstance)
                        .map(CaughtExceptionExpr.class::cast)
                        .anyMatch(e -> e.getType().equals(type))) {
                    break;
                }

                cfg.getSuccessors(popped)
                        .filter(e -> !visited.contains(e))
                        .forEach(e -> {
                            e.setStack(popped.getStack().copy());
                            bucket.add(e);
                        });
            }
        }

        for (BasicBlock vertex : cfg.vertices()) {
            if (vertex.getStack() == null) {
                vertex.setStack(
                        new SkidTypeStack(
                                cfg.getLocals().getMaxStack(),
                                skidfuscator
                        )
                );
            }
        }
		/*visited.clear();
		bucket.add(entry);

		ExpressionStack stack = new ExpressionStack(cfg.getLocals().getMaxStack() + 1);

		entry.setStack(stack);

		while (!bucket.isEmpty()) {
			/* Visit the top of the stack *//*
			final BasicBlock popped = bucket.pop();
			visited.add(popped);

			/* Iterate all the set statements to update the frame *//*
			final ExpressionStack expressionPool = popped.getStack().copy();

			AtomicReference<ExceptionRange<BasicBlock>> range = null;
			iteration: {
				for (Stmt stmt : popped) {
					for (Expr expr : stmt.enumerateOnlyChildren()) {
						if (expr instanceof CaughtExceptionExpr) {
							expressionPool.pop();
						}
						if (stmt instanceof ThrowStmt) {
							final ThrowStmt throwStmt = (ThrowStmt) stmt;
							expressionPool.push(throwStmt.getExpression());


							cfg.getEdges(popped)
									.stream()
									.filter(TryCatchEdge.class::isInstance)
									.map(TryCatchEdge.class::cast)
									.findFirst()
									.ifPresent(e -> range.set(e.erange));

							if (range == null) {
								break iteration;
							}


							range.get().getHandler().setStack(expressionPool);

						}
					}
				}

				/* Add all the successor nodes *//*
				cfg.getSuccessors(popped)
						.filter(e -> !visited.contains(e))
						.forEach(e -> {
							/* Set the expected received pool
							e.setPool(expressionPool);

							/* Add it to the stack to be iterated again
							bucket.add(e);
						});
			}
		}*/
    }

    private Type getRangeType(final ExceptionRange<BasicBlock> er) {
        Type type;
        Set<Type> typeSet = er.getTypes();
        if (typeSet.size() != 1) {
            // TODO: find base exception
            final Set<ClassNode> classNodes = new HashSet<>();
            for (Type typec : er.getTypes()) {
                final String type1 = typec.getClassName().replace(".", "/");
                ClassNode classNode = skidfuscator
                        .getClassSource()
                        .findClassNode(type1);

                if (classNode == null) {
                    System.err.println("\r\nFailed to find class of type " + type1  + "!\n" );
                    try {
                        final ClassReader reader = new ClassReader(type1);
                        final org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
                        reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                        classNode = new SkidClassNode(node, skidfuscator);
                        skidfuscator.getClassSource().getClassTree().addVertex(classNode);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                classNodes.add(classNode);
            }

            /* Simple DFS naive common ancestor algorithm */
            final Collection<ClassNode> commonAncestors = skidfuscator
                    .getClassSource()
                    .getClassTree()
                    .getCommonAncestor(classNodes);

            assert commonAncestors.size() > 0 : "No common ancestors between exceptions!";
            ClassNode mostIdealAncestor = null;

            if (commonAncestors.size() == 1) {
                mostIdealAncestor = commonAncestors.iterator().next();
            } else {
                iteration: {
                    for (ClassNode commonAncestor : commonAncestors) {
                        Stack<ClassNode> parents = new Stack<>();
                        parents.add(commonAncestor);

                        while (!parents.isEmpty()) {
                            if (parents.peek()
                                    .getName()
                                    .equalsIgnoreCase("java/lang/Throwable")) {
                                mostIdealAncestor = commonAncestor;
                                break iteration;
                            }

                            parents.addAll(skidfuscator.getClassSource().getClassTree().getParents(parents.pop()));
                        }
                    }
                }
            }

            assert mostIdealAncestor != null : "Exception parent not found in dumper!";

            type = Type.getObjectType(mostIdealAncestor.getName());
        } else {
            type = typeSet.iterator().next();
        }

        return type;
    }
}
