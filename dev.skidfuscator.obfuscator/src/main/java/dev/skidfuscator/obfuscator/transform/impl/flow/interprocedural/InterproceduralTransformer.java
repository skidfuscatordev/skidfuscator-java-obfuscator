package dev.skidfuscator.obfuscator.transform.impl.flow.interprocedural;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.clazz.InitClassTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.group.InitGroupTransformEvent;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.hierarchy.matching.ClassMethodHash;
import dev.skidfuscator.obfuscator.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowSetter;
import dev.skidfuscator.obfuscator.predicate.opaque.BlockOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.*;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.expr.SkidIntegerParseStaticInvocationExpr;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.OpcodeUtil;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.FieldStoreStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class InterproceduralTransformer extends AbstractTransformer {
    public InterproceduralTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "Interprocedural");
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
        if (entryPoint || skidGroup.isStatical()) {
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

            if ((methodNode.node.access & Opcodes.ACC_VARARGS) != 0) {
                methodNode.node.access &= ~Opcodes.ACC_VARARGS;
            }

            final ClassMethodHash classMethodHash = new ClassMethodHash(
                    skidMethodNode.getName(),
                    parameterGroup.getDesc(),
                    skidMethodNode.getOwner()
            );

            //System.out.println("Group: " + skidGroup.getName() + " Method: " + skidMethodNode.getName() + " Desc: " + parameterGroup.getDesc());
            //System.out.println(skidfuscator.getHierarchy().getGroups().stream().map(SkidGroup::toString).collect(Collectors.joining("\n")));
            if (skidfuscator.getHierarchy().getGroup(classMethodHash) != null && !skidGroup.getName().contains("<")) {
                //System.out.println("FOUND! Group: " + skidGroup.getName() + " Method: " + skidMethodNode.getName() + " Desc: " + parameterGroup.getDesc());
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
        skidGroup.setDesc(parameterGroup.getDesc());
        skidGroup.setStackHeight(finalStackHeight);
        skidGroup.setInjectedMethodPredicate(true);
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

        final MethodOpaquePredicate methodPredicate = methodNode.getPredicate();

        if (methodPredicate == null)
            return;

        methodPredicate.setGetter(vertex -> {
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

                expr = vertex1 -> new SkidIntegerParseStaticInvocationExpr(randomSeed);
            } else {
                seed = classPredicate.get();
                expr = classPredicate.getGetter();
            }

            if (skidMethodNode.getGroup().isInjectedMethodPredicate()) {
                seed = seed ^ skidMethodNode.getGroup().getPredicate().getPublic();

                final PredicateFlowGetter previousExprGetter = expr;
                expr = vertex2 -> {
                    final ControlFlowGraph cfg = vertex2.getGraph();

                    return new ArithmeticExpr(
                            /* Get the seed from the parameter */
                            new VarExpr(
                                    cfg.getLocals().get(skidMethodNode.getGroup().getStackHeight()),
                                    Type.INT_TYPE
                            ),
                            /* Hash the previous instruction */
                            previousExprGetter.get(vertex2),
                            /* Obv xor operation */
                            ArithmeticExpr.Operator.XOR
                    );
                };
            }

            return numberTransformer.getNumber(
                    methodPredicate.getPrivate(),
                    seed,
                    vertex,
                    expr
            );
        });
    }
}
