package dev.skidfuscator.obf.asm;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform_legacy.parameter.ParameterResolver;
import lombok.Data;
import lombok.Getter;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class MethodWrapper {
    protected final MethodNode methodNode;
    protected final MethodGroup methodGroup;

    public MethodWrapper(MethodNode methodNode, MethodGroup methodGroup) {
        this.methodNode = methodNode;
        this.methodGroup = methodGroup;
    }

    public void renderCallers(final SkidSession skidSession, final ParameterResolver resolver) {
        final Set<MethodNode> inheritence = skidSession
                .getCxt()
                .getInvocationResolver()
                .getHierarchyMethodChain(methodNode.owner, methodNode.getName(), methodNode.getDesc(), true);
        inheritence.add(methodNode);

        final Set<String> owners = inheritence.stream().map(e -> e.owner.getName()).collect(Collectors.toSet());

        skidSession.getIrFactory().values().stream()
                .parallel()
                .forEach(cfg -> {
                    methodGroup.callers.addAll(cfg
                            .allExprStream()
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
                                    final DynamicInvocationExpr expr = (DynamicInvocationExpr) e;

                                    // Todo!
                                    return null;
                                } else {
                                    return new Invocation(e, e.getOwner(), e.getName(), e.getDesc());
                                }
                            })
                            .map(e -> {
                                final MethodWrapper wrapper = resolver.getWrapper(cfg.getOwner(), cfg.getName(), cfg.getDesc());
                                final MethodWrapper called = resolver.getWrapper(e.getOwner(), e.getName(), e.getDesc());

                                return new MethodInvocation(wrapper, called, e.getExpr());
                            })
                            .collect(Collectors.toSet()));
        });

    }

    public MethodNode render() {
        this.methodNode.node.name = methodGroup.getName();
        this.methodNode.node.desc = methodGroup.getDesc().getDesc();

        return methodNode;
    }

    public String getDisplayName() {
        return methodNode.owner.getDisplayName() + "#" + methodNode.getDisplayName();
    }

    public String getHash() {
        return methodNode.owner.getDisplayName() + "#" + methodNode.getDisplayName() + methodNode.getDesc();
    }

    @Data
    protected static class Invocation {
        private final InvocationExpr expr;
        private final String owner;
        private final String name;
        private final String desc;

        public String getDisplayName() {
            return owner + "#" + name + desc;
        }
    }
}
