package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.attribute.AttributeKey;
import dev.skidfuscator.obfuscator.attribute.AttributeMap;
import dev.skidfuscator.obfuscator.attribute.StandardAttribute;
import dev.skidfuscator.obfuscator.predicate.opaque.MethodOpaquePredicate;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.objectweb.asm.Handle;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class SkidGroup {
    private final List<MethodNode> methodNodeList;
    private final Skidfuscator skidfuscator;
    private final MethodOpaquePredicate predicate;
    private final Set<SkidInvocation> invokers;
    private final AttributeMap attributes;

    private boolean annotation;
    private boolean statical;
    private boolean synthetic;
    private boolean natived;
    private boolean mixin;
    private boolean enumerator;
    private String name;
    private String desc;

    private int stackHeight;
    private transient boolean application;

    // TODO: Add parameter and parameter compilation

    public SkidGroup(List<MethodNode> methodNodeList, Skidfuscator skidfuscator) {
        this.methodNodeList = methodNodeList;
        this.skidfuscator = skidfuscator;
        this.invokers = new HashSet<>();
        this.predicate = skidfuscator
                .getPredicateAnalysis()
                .getMethodPredicate(this);

        this.application = methodNodeList
                .stream()
                .allMatch(e -> skidfuscator.getClassSource().isApplicationClass(e.owner.getName())
                        && !skidfuscator.getExemptAnalysis().isExempt(e)
                        && !skidfuscator.getExemptAnalysis().isExempt(e.owner)
                );

        this.enumerator = methodNodeList
                .stream()
                .anyMatch(e -> e.owner.isEnum());

        this.natived = methodNodeList
                .stream()
                .anyMatch(e -> e.owner.isNative());

        this.mixin = methodNodeList
                .stream()
                .filter(e -> e.owner instanceof SkidClassNode)
                .map(e -> e.owner)
                .map(SkidClassNode.class::cast)
                .anyMatch(SkidClassNode::isMixin);

        this.attributes = new AttributeMap();
    }

    public boolean hasAttribute(AttributeKey attributeKey) {
        return attributes.containsKey(attributeKey);
    }

    public <T> T getAttribute(AttributeKey attributeKey) {
        return attributes.poll(attributeKey);
    }

    public <T> void setAttribute(AttributeKey attributeKey, T value) {
        attributes.get(attributeKey).set(value);
    }

    public <T> void addAttribute(AttributeKey attributeKey, T value) {
        attributes.put(attributeKey, new StandardAttribute<>(value));
    }

    public void setStatical(boolean statical) {
        assert !statical || methodNodeList.size() == 1 : "Method group has multiple methods despite being static";

        this.statical = statical;
    }

    public void setSynthetic(boolean synthetic) {
        this.synthetic = synthetic;
    }

    public boolean isInit() {
        return name.equals("<init>");
    }

    public boolean isClinit() {
        return name.equals("<clinit>");
    }

    public void setDesc(final String desc) {
        this.desc = desc;

        for (MethodNode methodNode : methodNodeList) {
            methodNode.node.desc = desc;
        }

        for (SkidInvocation invoker : invokers) {
            invoker.getExpr().setDesc(desc);
        }
    }

    public void setName(final String name) {
        this.name = name;

        for (MethodNode methodNode : methodNodeList) {
            methodNode.node.name = name;
        }

        for (SkidInvocation invoker : invokers) {
            if (invoker.isDynamic()) {
                final DynamicInvocationExpr expr = (DynamicInvocationExpr) invoker.asExpr();
                final Handle boundFunc = (Handle) expr.getBootstrapArgs()[1];
                final Handle updatedBoundFunc = new Handle(
                        boundFunc.getTag(),
                        boundFunc.getOwner(),
                        name,
                        boundFunc.getDesc(),
                        boundFunc.isInterface()
                );

                expr.getBootstrapArgs()[1] = updatedBoundFunc;
            } else {
                invoker.getExpr().setName(name);
            }
        }
    }

    public MethodNode first() {
        return methodNodeList.iterator().next();
    }

    public boolean isEntryPoint() {
        return !application
                || this.getInvokers().isEmpty()
                || this.getInvokers().stream().anyMatch(SkidInvocation::isDynamic)
                || this.isAnnotation()
                || this.isEnumerator();
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
