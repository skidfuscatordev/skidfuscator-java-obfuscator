package dev.skidfuscator.obfuscator.skidasm.builder;

import dev.skidfuscator.builder.MethodNodeBuilder;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidGroup;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import org.mapleir.app.factory.Builder;
import org.objectweb.asm.tree.MethodNode;

public class SkidMethodNodeBuilder implements Builder<SkidMethodNode> {
    private final Skidfuscator skidfuscator;
    private final SkidClassNode classNode;
    private final MethodNodeBuilder methodNodeBuilder;
    private boolean phantom;

    public SkidMethodNodeBuilder(Skidfuscator skidfuscator, SkidClassNode classNode) {
        this.skidfuscator = skidfuscator;
        this.classNode = classNode;
        this.methodNodeBuilder = new MethodNodeBuilder();
    }

    public SkidMethodNodeBuilder access(int access) {
        methodNodeBuilder.access(access);
        return this;
    }

    public SkidMethodNodeBuilder name(String name) {
        methodNodeBuilder.name(name);
        return this;
    }

    public SkidMethodNodeBuilder desc(String desc) {
        methodNodeBuilder.desc(desc);
        return this;
    }

    public SkidMethodNodeBuilder signature(String signature) {
        methodNodeBuilder.signature(signature);
        return this;
    }

    public SkidMethodNodeBuilder exceptions(String[] exceptions) {
        methodNodeBuilder.exceptions(exceptions);
        return this;
    }

    public SkidMethodNodeBuilder phantom(boolean value) {
        this.phantom = value;
        return this;
    }


    @Override
    public SkidMethodNode build() {
        final MethodNode methodNode = methodNodeBuilder.build();
        final SkidMethodNode skidMethodNode = new SkidMethodNode(methodNode, classNode, classNode.getSkidfuscator());

        if (!phantom) {
            final SkidGroup group = classNode.getSkidfuscator().getHierarchy().cache(skidMethodNode);
            classNode.getMethods().add(skidMethodNode);
            skidMethodNode.setGroup(group);
        }

        classNode.node.methods.add(methodNode);

        return skidMethodNode;
    }
}
