package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.ClassReader;
import org.topdank.byteengineer.commons.asm.ASMFactory;
import org.topdank.byteengineer.commons.asm.DefaultASMFactory;

public class SkidASMFactory extends DefaultASMFactory {
    private final Skidfuscator skidfuscator;

    public SkidASMFactory(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }

    @Override
    public ClassNode create(byte[] bytes, String name) {
        final ClassReader reader = new ClassReader(bytes);
        final org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return new SkidClassNode(node, skidfuscator);
    }
}
