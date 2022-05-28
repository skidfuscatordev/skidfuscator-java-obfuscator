package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.ArrayList;
import java.util.List;

@Data
public class SkidGroup {
    private final List<MethodNode> methodNodeList;
    private final Skidfuscator skidfuscator;
    private final MethodOpaquePredicate predicate;
    private final List<SkidInvocation> invokers;

    private boolean annotation;
    private boolean statical;
    private String name;
    private String desc;

    private int stackHeight;
    private transient boolean application;

    // TODO: Add parameter and parameter compilation

    public SkidGroup(List<MethodNode> methodNodeList, Skidfuscator skidfuscator) {
        this.methodNodeList = methodNodeList;
        this.skidfuscator = skidfuscator;
        this.invokers = new ArrayList<>();
        this.predicate = skidfuscator
                .getPredicateAnalysis()
                .getMethodPredicate(this);

        this.application = methodNodeList
                .stream()
                .allMatch(e -> skidfuscator.getClassSource().isApplicationClass(e.owner.getName())
                        && !skidfuscator.getExemptAnalysis().isExempt(e)
                        && !skidfuscator.getExemptAnalysis().isExempt(e.owner)
                );
    }

    public void setStatical(boolean statical) {
        assert !statical || methodNodeList.size() == 1 : "Method group has multiple methods despite being static";

        this.statical = statical;
    }

    public void setDesc(final String desc) {
        for (MethodNode methodNode : methodNodeList) {
            methodNode.node.desc = desc;
        }

        for (SkidInvocation invoker : invokers) {
            invoker.getExpr().setDesc(desc);
        }
    }

    public void setName(final String name) {
        for (MethodNode methodNode : methodNodeList) {
            methodNode.node.name = name;
        }

        for (SkidInvocation invoker : invokers) {
            invoker.getExpr().setName(name);
        }
    }

    public MethodNode first() {
        return methodNodeList.iterator().next();
    }

    public boolean isEntryPoint() {
        return !application || this.getInvokers().isEmpty() || this.isAnnotation();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkidGroup skidGroup = (SkidGroup) o;

        return methodNodeList.equals(skidGroup.methodNodeList);
    }

    @Override
    public int hashCode() {
        return methodNodeList.hashCode();
    }
}
