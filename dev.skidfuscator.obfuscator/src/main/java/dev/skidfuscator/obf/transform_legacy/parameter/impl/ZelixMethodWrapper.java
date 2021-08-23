package dev.skidfuscator.obf.transform_legacy.parameter.impl;

import dev.skidfuscator.obf.asm.MethodWrapper;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform_legacy.number.NumberManager;
import dev.skidfuscator.obf.transform_legacy.parameter.ParameterResolver;
import dev.skidfuscator.obf.utils.OpcodeUtil;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.impl.BasicLocal;
import org.objectweb.asm.Type;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class ZelixMethodWrapper extends MethodWrapper {
    public ZelixMethodWrapper(MethodNode methodNode, ZelixMethodGroup methodGroup) {
        super(methodNode, methodGroup);
    }
    private static final Logger LOGGER = Logger.getLogger(ZelixMethodWrapper.class);
    private Local local;

    @Override
    public ZelixMethodGroup getMethodGroup() {
        return (ZelixMethodGroup) methodGroup;
    }

    public void renderKey(final SkidSession session) {
        final ControlFlowGraph cfg = session.getIrFactory().get(methodNode);

        if (cfg == null) {
            System.out.println("Method node of id " + methodNode.owner.getDisplayName() + "#" + methodNode.getName() + " does not have a cfg");
            return;
        }

        final BasicLocal local = this.getMethodGroup().isRoot()
                ? createSeed(cfg)
                : getSeed(cfg);

        this.local = local;
    }

    @Override
    public void renderCallers(final SkidSession skidSession, final ParameterResolver resolver) {
        final Set<MethodNode> inheritence = skidSession
                .getCxt()
                .getInvocationResolver()
                .getHierarchyMethodChain(methodNode.owner, methodNode.getName(), methodNode.getDesc(), true);

        final Set<String> owners = inheritence.stream().map(e -> e.owner.getName()).collect(Collectors.toSet());

        skidSession.getIrFactory().values().stream()
                .parallel()
                .forEach(cfg -> {
                    cfg.allExprStream()
                            .filter(e -> e instanceof InvocationExpr)
                            .map(e -> (InvocationExpr) e)
                            .filter(e -> {
                                if (e instanceof DynamicInvocationExpr) {
                                    // Todo!
                                    return false;
                                } else {
                                    if (!owners.contains(e.getOwner()))
                                        return false;
                                    if (!e.getName().equals(methodNode.getName()))
                                        return false;
                                    if (!e.getDesc().equals(methodNode.getDesc()))
                                        return false;

                                    return true;
                                }
                            })
                            .map(e -> {
                                if (e instanceof DynamicInvocationExpr) {
                                    // Todo!
                                    return null;
                                } else {
                                    return new Invocation(e, e.getOwner(), e.getName(), e.getDesc());
                                }
                            })
                            .distinct()
                            .filter(e -> {
                                final ZelixMethodWrapper wrapper = (ZelixMethodWrapper) resolver.getWrapper(cfg.getOwner(), cfg.getName(), cfg.getDesc());
                                final ZelixMethodWrapper called = (ZelixMethodWrapper) resolver.getWrapper(e.getOwner(), e.getName(), e.getDesc());

                                if (wrapper == null && called == null) {
                                    LOGGER.warn("[Caller] No caller and called found for "
                                            + cfg.getOwner() + "#" + cfg.getName() + cfg.getDesc() + " calling " + e.getDisplayName());
                                }
                                return wrapper != null && called != null && !called.methodNode.getName().contains("<");
                            })
                            .map(e -> {
                                final ZelixMethodWrapper wrapper = (ZelixMethodWrapper) resolver.getWrapper(cfg.getOwner(), cfg.getName(), cfg.getDesc());
                                final ZelixMethodWrapper called = (ZelixMethodWrapper) resolver.getWrapper(e.getOwner(), e.getName(), e.getDesc());


                                LOGGER.info("[Caller] Found caller of " + called.getDisplayName() + " in " + wrapper.getDisplayName());

                                return new ZelixInvocation(wrapper, called, e.getExpr());
                            })
                            .forEach(e -> methodGroup.getCallers().add(e));
                });

    }

    private BasicLocal createSeed(final ControlFlowGraph cfg) {
        final BasicLocal local = cfg.getLocals().getNextFreeLocal(false);

        /*
         * Here we initialize the private seed as a root factor. This is the sensitive part of the application
         * we'll have to rework it to add some protection
         */
        final ConstantExpr privateSeed = new ConstantExpr(this.getMethodGroup().getPrivateKey());
        final VarExpr privateSeedLoader = new VarExpr(local, Type.LONG_TYPE);
        final CopyVarStmt privateSeedSetter = new CopyVarStmt(privateSeedLoader, privateSeed);
        cfg.verticesInOrder().get(0).add(0, privateSeedSetter);

        return local;
    }

    private BasicLocal getSeed(final ControlFlowGraph cfg) {
        final BasicLocal local = cfg.getLocals().getNextFreeLocal(false);

        int stackHeight = Type.getArgumentTypes(this.getMethodGroup().getDesc().getDesc()).length;
        if (OpcodeUtil.isStatic(this.getMethodNode())) stackHeight -= 1;

        /*
         * We will always place the long local as the final one, hence we load the last parameter
         */
        final VarExpr publicSeed = new VarExpr(cfg.getLocals().get(stackHeight), Type.LONG_TYPE);

        /*
         * We then modify through arbitrary bitwise operations the public seed to the private seed
         */
        final Expr privateSeed = NumberManager.transform(this.getMethodGroup().getPrivateKey(),
                this.getMethodGroup().getPublicKey(), publicSeed);

        /*
         * We create a variable to store it then proceed to store it
         */
        final VarExpr privateSeedLoader = new VarExpr(local, Type.LONG_TYPE);
        final CopyVarStmt privateSeedSetter = new CopyVarStmt(privateSeedLoader, privateSeed);

        cfg.verticesInOrder().get(0).add(0, privateSeedSetter);

        return local;
    }
}
