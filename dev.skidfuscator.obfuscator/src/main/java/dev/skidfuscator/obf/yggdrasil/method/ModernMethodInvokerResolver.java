package dev.skidfuscator.obf.yggdrasil.method;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.yggdrasil.method.hash.InvokerHash;
import org.apache.log4j.Logger;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
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

public class ModernMethodInvokerResolver implements MethodInvokerResolver {
    private static final Logger LOGGER = Logger.getLogger(ModernMethodInvokerResolver.class);
    private final SkidSession app;

    public ModernMethodInvokerResolver(SkidSession app) {
        this.app = app;
        this.computeVStructure();
    }

    private final Map<MethodNode, Set<MethodNode>> heredityMap = new ConcurrentHashMap<>();
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
        this.app.getClassSource().getClassTree().vertices().stream()
                .filter(e -> app.getClassSource().isApplicationClass(e.getName()))
                .forEach(clazz -> {
            clazz.getMethods().forEach(method -> {
                //LOGGER.info("Adding " + method.owner.getName() + "#" + method.getName() + method.getDesc());
                this.methodToInvokerMap.put(method, new ArrayList<>());
                this.methodInvokers.put(method, new ArrayList<>());

                final Set<MethodNode> callers = app.getCxt()
                        .getInvocationResolver()
                        .getHierarchyMethodChain(clazz, method.getName(), method.getDesc(), false);

                this.heredityMap.put(method, callers);
            });
        });

        this.app.getClassSource().getClassTree().vertices().stream()
                .filter(e -> app.getClassSource().isApplicationClass(e.getName()))
                .forEach(clazz -> {
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

        controlFlowGraph.allExprStream().parallel()
                .filter(e -> e instanceof InvocationExpr)
                .filter(e -> !(e instanceof DynamicInvocationExpr))
                .map(e -> (InvocationExpr) e)
                .forEach(ex -> {
                    final ClassNode classNode = app.getCxt().getApplication().findClassNode(ex.getOwner());
                    if (classNode == null)
                        return;


        });


    }
}
