package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.topdank.byteengineer.commons.asm.DefaultASMFactory;

public class SkidLibASMFactory extends DefaultASMFactory {
    private final Skidfuscator skidfuscator;

    public SkidLibASMFactory(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }

    @Override
    public ClassNode create(byte[] bytes, String name) {
        final ClassReader reader = new ClassReader(bytes);
        final org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

        for (int i = 0; i < node.methods.size(); i++) {
            final org.objectweb.asm.tree.MethodNode methodNode = node.methods.get(i);
            final JSRInlinerAdapter adapter = new JSRInlinerAdapter(
                    methodNode,
                    methodNode.access,
                    methodNode.name,
                    methodNode.desc,
                    methodNode.signature,
                    methodNode.exceptions.toArray(new String[0])
            );
            methodNode.accept(adapter);
            node.methods.set(i, adapter);
        }

        return new SkidClassNode(node, skidfuscator);
    }
}
