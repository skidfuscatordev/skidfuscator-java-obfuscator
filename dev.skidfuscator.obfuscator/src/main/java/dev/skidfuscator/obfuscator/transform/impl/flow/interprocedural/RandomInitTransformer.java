package dev.skidfuscator.obfuscator.transform.impl.flow.interprocedural;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.clazz.InitClassTransformEvent;
import dev.skidfuscator.obfuscator.number.encrypt.impl.XorNumberTransformer;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidFieldNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.fake.FakeArithmeticExpr;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ArithmeticExpr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.VirtualInvocationExpr;
import org.mapleir.ir.code.stmt.FieldStoreStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Random;

public class RandomInitTransformer extends AbstractTransformer {

    public RandomInitTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Interprocedural Harden");
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

        final boolean skip = classNode.isInterface()
                || classNode.isAnnotation()
                || classNode.isEnum();

        if (skip) {
            this.skip();
            return;
        }


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

        clazzStaticPredicate.setGetter(vertex -> new FieldLoadExpr(
                null,
                classNode.node.name,
                staticFieldNode.node.name,
                staticFieldNode.node.desc,
                true
        ));
        clazzStaticPredicate.setSetter(expr -> new FieldStoreStmt(
                null,
                expr,
                classNode.node.name,
                staticFieldNode.node.name,
                staticFieldNode.node.desc,
                true
        ));

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

        clazzInstancePredicate.setGetter(vertex -> new FieldLoadExpr(
                /* Call the class this variable */
                new VarExpr(
                        vertex.cfg.getLocals().get(0),
                        classNode.getType()
                ),
                classNode.node.name,
                fieldNode.node.name,
                fieldNode.node.desc,
                false
        ));
        clazzInstancePredicate.setSetter(expr -> new FieldStoreStmt(
                new VarExpr(
                        expr.getBlock().cfg.getLocals().get(0),
                        Type.getType("L" + classNode.getName() + ";")
                ),
                expr,
                classNode.node.name,
                fieldNode.node.name,
                fieldNode.node.desc,
                false
        ));


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
    }
}
