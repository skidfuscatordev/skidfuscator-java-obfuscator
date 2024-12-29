package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.Invokable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

@Data
public class SkidInvocation {
    private final MethodNode owner;
    private final Type type;
    private Invokable expr;
    private InvokeDynamicInsnNode invokeDynamicInsnNode;
    private MethodInsnNode methodNode;
    private boolean tainted;

    private boolean exempt;

    public SkidInvocation(MethodNode owner, Invokable expr) {
        this.owner = owner;
        this.expr = expr;
        this.type = Type.EXPR;
    }

    public SkidInvocation(MethodNode owner, MethodInsnNode methodNode) {
        this.owner = owner;
        this.methodNode = methodNode;
        this.type = Type.METHOD;
    }

    public SkidInvocation(MethodNode owner, InvokeDynamicInsnNode invokeDynamicInsnNode) {
        this.owner = owner;
        this.invokeDynamicInsnNode = invokeDynamicInsnNode;
        this.type = Type.METHOD_DYNAMIC;
    }

    public void setName(final String name) {
        switch (type) {
            case EXPR:
                if (isDynamic()) {
                    final DynamicInvocationExpr expr = (DynamicInvocationExpr) this.asExpr();
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
                    getExpr().setName(name);
                }
                break;
            case METHOD:
                methodNode.name = name;
                break;
            case METHOD_DYNAMIC:
                final Handle boundFunc = (Handle) invokeDynamicInsnNode.bsmArgs[1];
                final Handle updatedBoundFunc = new Handle(
                        boundFunc.getTag(),
                        boundFunc.getOwner(),
                        name,
                        boundFunc.getDesc(),
                        boundFunc.isInterface()
                );
                invokeDynamicInsnNode.bsmArgs[1] = updatedBoundFunc;
                break;
        }
    }

    public void setDesc(final String desc) {
        switch (type) {
            case EXPR:
                if (isDynamic()) {
                    final DynamicInvocationExpr expr = (DynamicInvocationExpr) this.asExpr();
                    final Handle boundFunc = (Handle) expr.getBootstrapArgs()[1];
                    final Handle updatedBoundFunc = new Handle(
                            boundFunc.getTag(),
                            boundFunc.getOwner(),
                            boundFunc.getName(),
                            desc,
                            boundFunc.isInterface()
                    );

                    expr.getBootstrapArgs()[1] = updatedBoundFunc;
                } else {
                    getExpr().setDesc(desc);
                }
                break;
            case METHOD:
                Skidfuscator.LOGGER.warn("This method is exempted! This is dangerous behaviour...\n");
                methodNode.name = desc;
                break;
            case METHOD_DYNAMIC:
                Skidfuscator.LOGGER.warn("This method is exempted! This is dangerous behaviour...\n");
                final Handle boundFunc = (Handle) invokeDynamicInsnNode.bsmArgs[1];
                final Handle updatedBoundFunc = new Handle(
                        boundFunc.getTag(),
                        boundFunc.getOwner(),
                        boundFunc.getName(),
                        desc,
                        boundFunc.isInterface()
                );
                invokeDynamicInsnNode.bsmArgs[1] = updatedBoundFunc;
                break;
        }
    }

    public Invokable getExpr() {
        return expr;
    }

    public void setExpr(Invokable expr) {
        this.expr = expr;
    }

    public Invocation asExpr() {
        return (Invocation) expr;
    }

    public boolean isDynamic() {
        return expr instanceof DynamicInvocationExpr;
    }

    public boolean isTainted() {
        return tainted;
    }

    public void setTainted(boolean tainted) {
        this.tainted = tainted;
    }

    public boolean isExempt() {
        return exempt || methodNode != null || invokeDynamicInsnNode != null;
    }

    public void setExempt(boolean exempt) {
        this.exempt = exempt;
    }

    public enum Type {
        EXPR,
        METHOD,
        METHOD_DYNAMIC
    }
}
