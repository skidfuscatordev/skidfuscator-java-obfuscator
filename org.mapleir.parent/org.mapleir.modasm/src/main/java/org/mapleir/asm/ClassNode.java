package org.mapleir.asm;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class ClassNode implements FastGraphVertex {
    private static int ID_COUNTER = 1;
   	private final int numericId = ID_COUNTER++;

    public org.objectweb.asm.tree.ClassNode node;
    private final List<MethodNode> methods;
    private final List<FieldNode> fields;
    private boolean synth;

    public ClassNode() {
        this.node = new org.objectweb.asm.tree.ClassNode();
        methods = new ArrayList<>();
        fields = new ArrayList<>();
    }

    protected ClassNode(org.objectweb.asm.tree.ClassNode node, boolean compute) {
        this.node = node;

        methods = new ArrayList<>(node.methods.size());
        fields = new ArrayList<>(node.fields.size());

        if (compute) {
            for (org.objectweb.asm.tree.MethodNode mn : node.methods)
                methods.add(new MethodNode(mn, this));
            for (org.objectweb.asm.tree.FieldNode fn : node.fields)
                fields.add(new FieldNode(fn, this));
        }
    }

    ClassNode(org.objectweb.asm.tree.ClassNode node) {
        this.node = node;
        methods = new ArrayList<>(node.methods.size());
        for (org.objectweb.asm.tree.MethodNode mn : node.methods)
            methods.add(new MethodNode(mn, this));
        fields = new ArrayList<>(node.fields.size());
        for (org.objectweb.asm.tree.FieldNode fn : node.fields)
            fields.add(new FieldNode(fn, this));
    }

    public String getName() {
        return node.name;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    public void addMethod(MethodNode mn) {
        methods.add(mn);
        node.methods.add(mn.node);
    }

    public List<FieldNode> getFields() {
        return fields;
    }

    @Override
    public String getDisplayName() {
        return node.name.replace("/", "_");
    }

    @Override
    public int getNumericId() {
        return numericId;
    }

    public boolean isEnum() {
        return (node.access & Opcodes.ACC_ENUM) != 0;
    }

    public boolean isStatic() {
        return (node.access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isPublic() {
        return (node.access & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isProtected() {
        return (node.access & Opcodes.ACC_PROTECTED) != 0;
    }

    public boolean isPrivate() {
        return (node.access & Opcodes.ACC_PRIVATE) != 0;
    }

    public boolean isSynthetic() {
        return (node.access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public boolean isInterface() {
        return (node.access & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isNative() {
        return (node.access & Opcodes.ACC_NATIVE) != 0;
    }

    public boolean isAnnoyingVersion() {
        return (node.version & 0xFFFF) < Opcodes.V1_8;
    }

    public boolean isVirtual() {
        return synth;
    }

    public void setVirtual(boolean synth) {
        this.synth = synth;
    }

    @Override
    public String toString() {
        return "ClassNode{" +
                "node=" + node +
                '}';
    }
}
