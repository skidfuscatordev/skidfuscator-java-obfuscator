package dev.skidfuscator.obfuscator.skidasm.builder;

import dev.skidfuscator.builder.FieldNodeBuilder;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidFieldNode;
import org.mapleir.app.factory.Builder;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.List;

public class SkidFieldNodeBuilder implements Builder<SkidFieldNode> {
    private final Skidfuscator skidfuscator;
    private final SkidClassNode classNode;
    private final FieldNodeBuilder fieldNodeBuilder;

    public SkidFieldNodeBuilder(Skidfuscator skidfuscator, SkidClassNode classNode) {
        this.skidfuscator = skidfuscator;
        this.classNode = classNode;
        this.fieldNodeBuilder = new FieldNodeBuilder();
    }

    private boolean phantom;
    
    public SkidFieldNodeBuilder access(int access) {
        this.fieldNodeBuilder.access(access);
        return this;
    }

    public SkidFieldNodeBuilder name(String name) {
        this.fieldNodeBuilder.name(name);
        return this;
    }

    public SkidFieldNodeBuilder desc(String desc) {
        this.fieldNodeBuilder.desc(desc);
        return this;
    }

    public SkidFieldNodeBuilder signature(String signature) {
        this.fieldNodeBuilder.signature(signature);
        return this;
    }

    public SkidFieldNodeBuilder value(Object value) {
        this.fieldNodeBuilder.value(value);
        return this;
    }

    public SkidFieldNodeBuilder visibleAnnotations(List<AnnotationNode> visibleAnnotations) {
        this.fieldNodeBuilder.visibleAnnotations(visibleAnnotations);
        return this;
    }

    public SkidFieldNodeBuilder invisibleAnnotations(List<AnnotationNode> invisibleAnnotations) {
        this.fieldNodeBuilder.invisibleAnnotations(invisibleAnnotations);
        return this;
    }

    public SkidFieldNodeBuilder visibleTypeAnnotations(List<TypeAnnotationNode> visibleTypeAnnotations) {
        this.fieldNodeBuilder.visibleTypeAnnotations(visibleTypeAnnotations);
        return this;
    }

    public SkidFieldNodeBuilder invisibleTypeAnnotations(List<TypeAnnotationNode> invisibleTypeAnnotations) {
        this.fieldNodeBuilder.invisibleTypeAnnotations(invisibleTypeAnnotations);
        return this;
    }

    public SkidFieldNodeBuilder attrs(List<Attribute> attrs) {
        this.fieldNodeBuilder.attrs(attrs);
        return this;
    }

    @Override
    public SkidFieldNode build() {
        final FieldNode fieldNode = fieldNodeBuilder.build();
        final SkidFieldNode skidMethodNode = new SkidFieldNode(
                fieldNode,
                classNode,
                classNode.getSkidfuscator()
        );

        if (!phantom) {
            classNode.getFields().add(skidMethodNode);
        }

        classNode.node.fields.add(fieldNode);
        
        return skidMethodNode;
    }
}
