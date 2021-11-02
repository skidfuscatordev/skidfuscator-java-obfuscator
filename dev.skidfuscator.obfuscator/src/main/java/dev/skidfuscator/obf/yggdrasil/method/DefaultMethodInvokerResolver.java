package dev.skidfuscator.obf.yggdrasil.method;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.yggdrasil.method.hash.ClassMethodHash;
import dev.skidfuscator.obf.yggdrasil.method.hash.InvokerHash;
import org.apache.log4j.Logger;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Ghast
 * @since 08/03/2021
 * SkidfuscatorV2 © 2021
 */

public class DefaultMethodInvokerResolver implements MethodInvokerResolver {
    private static final Logger LOGGER = Logger.getLogger(DefaultMethodInvokerResolver.class);
    private final SkidSession app;

    public DefaultMethodInvokerResolver(SkidSession app) {
        this.app = app;
        this.computeVStructure();
    }

    private final Map<MethodNode, List<InvokerHash>> methodToInvokerMap = new ConcurrentHashMap<>();
    private final Map<MethodNode, List<InvokerHash>> methodInvokers = new ConcurrentHashMap<>();

    @Override
    public List<InvokerHash> getCallers(MethodNode methodNode) {
        return methodToInvokerMap.get(methodNode);
    }

    @Override
    public List<MethodNode> getNeverCalled() {
        return methodToInvokerMap.entrySet().stream().filter(e -> e.getValue().size() < 1)
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public List<InvokerHash> getCalled(MethodNode methodNode) {
        final List<InvokerHash> var = methodInvokers.get(methodNode);
        return var == null ? new ArrayList<>() : var;
    }

    private void computeVStructure() {
        this.methodToInvokerMap.clear();
        LOGGER.info("Iterating through " + this.app.getClassSource().getClassTree().size() + " classes");
        this.app.getClassSource().iterate().forEach(clazz -> {
            clazz.getMethods().forEach(method -> {
                //LOGGER.info("Adding " + method.owner.getName() + "#" + method.getName() + method.getDesc());
                this.methodToInvokerMap.put(method, new ArrayList<>());
                this.methodInvokers.put(method, new ArrayList<>());
            });
        });

        this.app.getClassSource().iterate().forEach(clazz -> {
            clazz.getMethods().forEach(m -> {
                try {
                    computeVTable(m);
                } catch (Exception e){
                    LOGGER.error("Failed for class " + m.owner.getName() + "#" + m.getName() + m.getDesc(), e);
                };
                //LOGGER.info("Added " + m.owner.getName() + "#" + m.getName() + m.getDesc());
            });
        });
    }

    private void computeVTable(final MethodNode methodNode) {
        final ControlFlowGraph controlFlowGraph = app.getCxt().getIRCache().getFor(methodNode);

        controlFlowGraph.allExprStream().parallel().filter(e -> e instanceof InvocationExpr)
                .map(e -> (InvocationExpr) e).forEach(ex -> {
                    try {
                        ex.resolveTargets(app.getCxt().getInvocationResolver()).forEach(target -> {
                            if (target == null)
                                return;

                            final Set<MethodNode> callers = app.getCxt().getInvocationResolver()
                                    .getHierarchyMethodChain(target.owner, target.getName(), target.getDesc(), false);

                            for (MethodNode caller : callers) {
                                final InvokerHash invokerHash = new InvokerHash(methodNode, ex);

                                if (methodToInvokerMap.containsKey(caller)) {
                                    this.methodToInvokerMap.get(caller).add(invokerHash);
                                }

                                final InvokerHash callerHash = new InvokerHash(caller, ex);
                                this.methodInvokers.get(methodNode).add(callerHash);
                            }
                        });
                    } catch (Throwable e) {
                        LOGGER.error("Failed for class " + methodNode.owner.getName() + "#" + methodNode.getName() + methodNode.getDesc()
                                + ":" + ex.toString(), e);
                    }
        });


    }
}
