package dev.skidfuscator.builder;

import org.mapleir.app.factory.Builder;
import org.objectweb.asm.tree.MethodNode;

public class MethodNodeBuilder implements Builder<MethodNode> {
    private int access = -1;
    private String name;
    private String desc;
    private String signature;
    private String[] exceptions = new String[0];

    public MethodNodeBuilder access(int access) {
        this.access = access;
        return this;
    }

    public MethodNodeBuilder name(String name) {
        this.name = name;
        return this;
    }

    public MethodNodeBuilder desc(String desc) {
        this.desc = desc;
        return this;
    }

    public MethodNodeBuilder signature(String signature) {
        this.signature = signature;
        return this;
    }

    public MethodNodeBuilder exceptions(String[] exceptions) {
        this.exceptions = exceptions;
        return this;
    }

    @Override
    public MethodNode build() {
        assert access != -1 : "Access has to be defined";
        assert name != null : "Name has to be defined!";
        assert desc != null : "Description has to be defined!";

        return new MethodNode(
                access,
                name,
                desc,
                signature,
                exceptions
        );
    }
}
