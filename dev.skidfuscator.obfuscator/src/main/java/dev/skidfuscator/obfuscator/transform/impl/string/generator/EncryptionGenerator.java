package dev.skidfuscator.obfuscator.transform.impl.string.generator;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;

public interface EncryptionGenerator {
    String encrypt(String input, int key);

    String decrypt(String input, int key);

    void visit(final SkidClassNode node, String name);
}
