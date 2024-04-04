package dev.skidfuscator.obfuscator.transform.impl.string.generator.v3;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import org.mapleir.ir.code.Expr;

import java.nio.charset.StandardCharsets;

public class BytesV3EncryptionGenerator extends AbstractEncryptionGeneratorV3 {
    private final byte[] keys;

    public BytesV3EncryptionGenerator(byte[] keys) {
        super("Bytes Generator");
        this.keys = keys;
    }

    private Expr internalKeys;

    @Override
    public void visitPre(SkidClassNode node) {
        super.visitPre(node);

        this.internalKeys = generateByteArrayGenerator(node, keys);
    }

    @Override
    public Expr encrypt(String input, SkidMethodNode node, SkidBlock block) {
        final byte[] encrypted = input.getBytes(StandardCharsets.UTF_16);

        // Super simple converting our integer to string, and getting bytes.
        final byte[] keyBytes = Integer.toString(node.getBlockPredicate(block)).getBytes();

        // Super simple XOR
        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] ^= keyBytes[i % keyBytes.length];
            encrypted[i] ^= keys[i % keys.length];
        }

        // Base64 encode it for testing
        return callInjectMethod(
                node.getParent(),
                "decryptor",
                "([B[BI)Ljava/lang/String;",
                generateByteArrayGenerator(node.getParent(), encrypted),
                internalKeys.copy(),
                node.getFlowPredicate().getGetter().get(block)
        );
    }

    @Override
    public String decrypt(DecryptorDictionary dictionary, int key) {
        final byte[] input = dictionary.get("encrypted");

        // Super simple converting our integer to string, and getting bytes.
        final byte[] keyBytes = Integer.toString(key).getBytes();

        // Super simple XOR
        for (int i = 0; i < input.length; i++) {
            input[i] ^= keyBytes[i % keyBytes.length];
            input[i] ^= keys[i % keys.length];
        }

        // Base64 encode it for testing
        return new String(input, StandardCharsets.UTF_16);
    }

    @InjectMethod(value = "decryptor", tags = InjectMethodTag.RANDOM_NAME)
    private static String decryptMeBitch(final byte[] input, final byte[] keys, final int key) {
        final byte[] keyBytes = Integer.toString(key).getBytes();

        // Super simple XOR
        for (int i = 0; i < input.length; i++) {
            input[i] ^= keyBytes[i % keyBytes.length];
            input[i] ^= keys[i % keys.length];
        }

        // Base64 encode it for testing
        return new String(input, StandardCharsets.UTF_16);
    }
}
