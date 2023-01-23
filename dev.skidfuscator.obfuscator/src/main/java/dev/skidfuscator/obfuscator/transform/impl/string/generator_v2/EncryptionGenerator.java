package dev.skidfuscator.obfuscator.transform.impl.string.generator_v2;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;

public interface EncryptionGenerator {
    byte[] encrypt(String input, int key);

    String decrypt(byte[] input, int key);

    void visit(final SkidClassNode node, String name);
}
