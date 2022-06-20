package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.FieldNode;

public class SkidFieldNode extends FieldNode {
    private final Skidfuscator skidfuscator;

    public SkidFieldNode(org.objectweb.asm.tree.FieldNode node, ClassNode owner, Skidfuscator skidfuscator) {
        super(node, owner);
        this.skidfuscator = skidfuscator;
    }
}
