package dev.skidfuscator.obf.transform_legacy.parameter.impl;

import dev.skidfuscator.obf.asm.MethodGroup;
import dev.skidfuscator.obf.asm.MethodWrapper;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform_legacy.number.NumberManager;
import dev.skidfuscator.obf.transform_legacy.parameter.Parameter;
import dev.skidfuscator.obf.transform_legacy.parameter.ParameterResolver;
import dev.skidfuscator.obf.yggdrasil.method.hash.ClassMethodHash;
import org.apache.log4j.Logger;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.VarExpr;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * @author Ghast
 * @since 09/03/2021
 * SkidfuscatorV2 © 2021
 */
public class ZelixParameterResolver implements ParameterResolver {
    private final SkidSession session;
    private static final Logger LOGGER = Logger.getLogger(ZelixMethodGroup.class);

    private final Map<ClassMethodHash, ZelixMethodWrapper> methodWrapperMap = new HashMap<>();
    private final Set<ZelixMethodGroup> methodGroups = new HashSet<>();

    public ZelixParameterResolver(SkidSession session) {
        this.session = session;
    }

    @Override
    public MethodWrapper getWrapper(String owner, String name, String desc) {
        return methodWrapperMap.get(new ClassMethodHash(name, desc, owner));
    }

    protected void initVTables() {
        final Random random = new Random();

        LOGGER.info("Beginning vertice evaluation...");
        session.getClassSource().iterate().forEach(cl -> {
            if (!session.getClassSource().isApplicationClass(cl.getName()))
                return;

            cl.getMethods().forEach(m -> {
                final ClassMethodHash classMethodHash = new ClassMethodHash(m);

                if (methodWrapperMap.containsKey(classMethodHash)) {
                    return;
                }

                LOGGER.info("[Vertice] Reading " + classMethodHash.getDisplayName());

                final ZelixMethodGroup methodGroup = new ZelixMethodGroup(
                        classMethodHash.getName(),
                        classMethodHash.getDesc(),
                        random.nextLong(),
                        random.nextLong()
                );

                final ZelixMethodWrapper methodWrapper = new ZelixMethodWrapper(
                        m,
                        methodGroup
                );

                methodGroup.getWrappers().add(methodWrapper);
                methodWrapperMap.put(classMethodHash, methodWrapper);

                final Set<MethodNode> hierachy = session.getCxt().getInvocationResolver()
                        .getHierarchyMethodChain(m.owner, m.getName(), m.getDesc(), false);

                for (MethodNode methodNode : hierachy) {
                    final ZelixMethodWrapper wrapper = new ZelixMethodWrapper(
                            methodNode,
                            methodGroup
                    );

                    methodGroup.getWrappers().add(wrapper);
                    final ClassMethodHash hash = new ClassMethodHash(methodNode);
                    methodWrapperMap.put(hash, wrapper);

                    LOGGER.info("[Vertice] Reading " + hash.getDisplayName());
                }

                methodGroups.add(methodGroup);

                /*final long privateKey = random.nextLong();
                final long publicKey = random.nextLong();

                for (MethodNode caller : hierachy) {
                    zelixMethodNodeMap.put(new ClassMethodHash(caller), new ZelixMethodNode(caller, privateKey, publicKey));
                }*/
            });
        });
        LOGGER.info("Finished vertice evaluation!");

        LOGGER.info("Beginning caller render...");
        for (MethodGroup methodGroup : methodGroups) {
            methodGroup.renderCallers(session, this);
        }
        LOGGER.info("Finished rendering all callers!");


        LOGGER.info("Beginning vertice rooter...");
        session.getEntryPoints().forEach(e -> {
            final ClassMethodHash classMethodHash = new ClassMethodHash(e);
            final ZelixMethodWrapper zelixMethodNode = this.methodWrapperMap.get(classMethodHash);
            if (zelixMethodNode == null){
                // Todo no idea what happens in this scenario
                System.out.println("Classnode of hash " + classMethodHash.getName() + " no has caller");
                return;
            }

            zelixMethodNode.getMethodGroup().setRoot(true);
        });
        LOGGER.info("Finished evaluating class roots!");

        LOGGER.info("Beginning vertice parameter render...");
        for (ZelixMethodGroup methodGroup : methodGroups) {
            if (methodGroup.isRoot()) continue;

            methodGroup.getDesc().addParameter(Type.LONG_TYPE);
        }

        for (ZelixMethodGroup methodGroup : methodGroups) {
            methodGroup.renderKey(session);
        }
        LOGGER.info("Finished rendering parameters!");

        LOGGER.info("Beginning caller changes...");
        final Set<ZelixInvocation> zelixInvocations = new HashSet<>();
        for (ZelixMethodGroup methodGroup : methodGroups) {
            LOGGER.info("Checking " + methodGroup.getCallers().size() + " callers");
            for (ZelixInvocation caller : methodGroup.getZelixCallers()) {

                if (zelixInvocations.contains(caller))
                    continue;

                final VarExpr privateSeed = new VarExpr(caller.getParent().getLocal(), Type.LONG_TYPE);
                /*
                 * Transform the seed for the next calling
                 */
                final Expr publicSeedCaller = NumberManager.transform(
                        caller.getParent().getMethodGroup().getPrivateKey(),
                        caller.getCalled().getMethodGroup().getPublicKey(),
                        privateSeed
                );

                final List<Expr> params = new ArrayList<>(Arrays.asList(caller.getExpr().getArgumentExprs()));
                params.add(publicSeedCaller);

                final Expr[] exprs = new Expr[params.size()];

                for (int i = 0; i < params.size(); i++) {
                    exprs[i] = params.get(i);
                }

                final Parameter parameter = new Parameter(caller.getExpr().getDesc());
                parameter.addParameter(Type.LONG_TYPE);

                caller.getExpr().setDesc(parameter.getDesc());
                caller.getExpr().setArgumentExprs(exprs);

                zelixInvocations.add(caller);

                LOGGER.info("[Caller] Changed variable in " + caller.getParent().getDisplayName()
                        + " calling "
                        + caller.getCalled().getDisplayName() + " with desc " + caller.getExpr().getDesc()
                        + " and params "
                        + Arrays.toString(caller.getExpr().getArgumentExprs()));
            }
        }
        LOGGER.info("Finished caller changes!...");
        LOGGER.info("Wrapping up final renders...");
        for (ZelixMethodGroup methodGroup : methodGroups) {
            methodGroup.render();
        }


    }
}
