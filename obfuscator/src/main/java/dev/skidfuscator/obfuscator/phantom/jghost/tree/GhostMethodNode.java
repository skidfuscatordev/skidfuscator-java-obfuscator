package dev.skidfuscator.obfuscator.phantom.jghost.tree;

import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

public class GhostMethodNode extends MethodNode {
    public GhostMethodNode(org.objectweb.asm.tree.MethodNode node, ClassNode owner) {
        super(node, owner);
    }
}
