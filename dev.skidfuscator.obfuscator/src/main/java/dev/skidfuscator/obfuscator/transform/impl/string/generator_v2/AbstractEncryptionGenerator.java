package dev.skidfuscator.obfuscator.transform.impl.string.generator_v2;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import org.objectweb.asm.ClassReader;

public abstract class AbstractEncryptionGenerator implements EncryptionGenerator{
    @Override
    public void visit(SkidClassNode node, String name) {
        // TODO: WIP but add a dynamic system which reads where to inject
        //       keys and shit
    }
}
