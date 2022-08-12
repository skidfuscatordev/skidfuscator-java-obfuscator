package dev.skidfuscator.builder;

import org.mapleir.app.factory.Builder;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.List;

public class FieldNodeBuilder implements Builder<FieldNode> {
    private int access = -1;
    private String name;
    private String desc;
    private String signature;
    private Object value;
    private List<AnnotationNode> visibleAnnotations;
    private List<AnnotationNode> invisibleAnnotations;
    private List<TypeAnnotationNode> visibleTypeAnnotations;
    private List<TypeAnnotationNode> invisibleTypeAnnotations;
    private List<Attribute> attrs;

    public FieldNodeBuilder access(int access) {
        this.access = access;
        return this;
    }

    public FieldNodeBuilder name(String name) {
        this.name = name;
        return this;
    }

    public FieldNodeBuilder desc(String desc) {
        this.desc = desc;
        return this;
    }

    public FieldNodeBuilder signature(String signature) {
        this.signature = signature;
        return this;
    }

    public FieldNodeBuilder value(Object value) {
        this.value = value;
        return this;
    }

    public FieldNodeBuilder visibleAnnotations(List<AnnotationNode> visibleAnnotations) {
        this.visibleAnnotations = visibleAnnotations;
        return this;
    }

    public FieldNodeBuilder invisibleAnnotations(List<AnnotationNode> invisibleAnnotations) {
        this.invisibleAnnotations = invisibleAnnotations;
        return this;
    }

    public FieldNodeBuilder visibleTypeAnnotations(List<TypeAnnotationNode> visibleTypeAnnotations) {
        this.visibleTypeAnnotations = visibleTypeAnnotations;
        return this;
    }

    public FieldNodeBuilder invisibleTypeAnnotations(List<TypeAnnotationNode> invisibleTypeAnnotations) {
        this.invisibleTypeAnnotations = invisibleTypeAnnotations;
        return this;
    }

    public FieldNodeBuilder attrs(List<Attribute> attrs) {
        this.attrs = attrs;
        return this;
    }

    @Override
    public FieldNode build() {
        assert access != -1 : "Access has not been set!";
        assert name != null : "Name of field cannot be null";
        assert desc != null : "Description of field cannot be null";

        final FieldNode fieldNode = new FieldNode(
                access,
                name,
                desc,
                signature,
                value
        );

        fieldNode.visibleAnnotations = visibleAnnotations;
        fieldNode.invisibleAnnotations = invisibleAnnotations;
        fieldNode.visibleTypeAnnotations = visibleTypeAnnotations;
        fieldNode.invisibleTypeAnnotations = invisibleTypeAnnotations;
        fieldNode.attrs = attrs;

        return fieldNode;
    }
}
