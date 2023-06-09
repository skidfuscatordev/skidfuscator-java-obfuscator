package dev.skidfuscator.obfuscator.skidasm.builder;

import dev.skidfuscator.builder.ClassNodeBuilder;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.impl.transform.clazz.InitClassTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import org.mapleir.app.factory.Builder;
import org.objectweb.asm.tree.ClassNode;

public class SkidClassNodeBuilder implements Builder<SkidClassNode> {
    private final Skidfuscator skidfuscator;
    private final ClassNodeBuilder classNodeBuilder;
    private boolean phantom;

    private boolean virtual = true;


    public SkidClassNodeBuilder(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
        this.classNodeBuilder = new ClassNodeBuilder();
    }

    public SkidClassNodeBuilder access(int access) {
        classNodeBuilder.access(access);
        return this;
    }

    public SkidClassNodeBuilder name(String name) {
        classNodeBuilder.name(name);
        return this;
    }

    public SkidClassNodeBuilder superName(String superName) {
        classNodeBuilder.superName(superName);
        return this;
    }

    public SkidClassNodeBuilder signature(String signature) {
        classNodeBuilder.signature(signature);
        return this;
    }

    public SkidClassNodeBuilder interfaces(String[] interfaces) {
        classNodeBuilder.interfaces(interfaces);
        return this;
    }

    public SkidClassNodeBuilder phantom(boolean value) {
        this.phantom = value;
        return this;
    }

    public SkidClassNodeBuilder virtual(boolean value) {
        this.virtual = value;
        return this;
    }

    @Override
    public SkidClassNode build() {
        final ClassNode classNode = classNodeBuilder.build();
        final SkidClassNode skidClassNode = new SkidClassNode(classNode, skidfuscator);
        skidClassNode.setVirtual(virtual);

        if (!phantom) {
            skidfuscator.getHierarchy().cache(skidClassNode);
            EventBus.call(new InitClassTransformEvent(skidfuscator, skidClassNode));
        }
        skidfuscator.getClassSource().add(skidClassNode);

        return skidClassNode;
    }
}
