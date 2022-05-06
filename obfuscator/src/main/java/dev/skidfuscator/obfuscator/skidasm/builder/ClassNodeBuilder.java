package dev.skidfuscator.obfuscator.skidasm.builder;

import org.mapleir.app.factory.Builder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.Arrays;

public class ClassNodeBuilder implements Builder<ClassNode> {
    public int access = -1;
    public String name;
    public String signature;
    public String superName;
    public String[] interfaces = new String[0];

    public ClassNodeBuilder access(int access) {
        this.access = access;
        return this;
    }

    public ClassNodeBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ClassNodeBuilder signature(String signature) {
        this.signature = signature;
        return this;
    }

    public ClassNodeBuilder superName(String superName) {
        this.superName = superName;
        return this;
    }

    public ClassNodeBuilder interfaces(String[] interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    @Override
    public ClassNode build() {
        assert access != -1 : "Access has to be defined";
        assert name != null : "Name has to be defined!";

        final ClassNode classNode = new ClassNode();

        classNode.name = this.name;
        classNode.access = this.access;
        classNode.version = Opcodes.V1_8;
        if (interfaces.length > 0) {
            classNode.interfaces = new ArrayList<>(Arrays.asList(interfaces));
        }
        classNode.signature = this.signature;
        classNode.superName = this.superName == null ? "java/lang/Object" : superName;
        return classNode;
    }
}
