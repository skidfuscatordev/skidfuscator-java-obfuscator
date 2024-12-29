package dev.skidfuscator.obfuscator.transform.impl.string.generator.algo;

import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.EncryptionGenerator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.objectweb.asm.Opcodes.*;

public class IvEncryptionGenerator implements EncryptionGenerator {
    private static final String KEY = "ComplexKey12345"; // Static key
    private static final String IV = "ComplexIV1234567"; // Static IV

    public IvEncryptionGenerator(String iv) {
        assert iv.length() == 16 : "AES IV must be 16 bytes long";
    }

    private static String transform(String input, boolean encrypt) {
        final int prime = 257; // Use a prime number for calculations
        int[] keyExpansion = new int[input.length()];
        int[] ivExpansion = new int[input.length()];
        for (int i = 0; i < input.length(); i++) {
            keyExpansion[i] = (KEY.charAt(i % KEY.length()) * prime) % 256;
            ivExpansion[i] = (IV.charAt(i % IV.length()) * prime) % 256;
        }

        StringBuilder output = new StringBuilder();
        int previousChar = 0;
        for (int i = 0; i < input.length(); i++) {
            int inputChar = input.charAt(i);
            int keyChar = keyExpansion[i];
            int ivChar = ivExpansion[i];
            int mix = (keyChar + ivChar + previousChar) % 256; // Mix with previous char for more complexity
            if (encrypt) {
                inputChar = (inputChar + mix) % 256;
            } else {
                inputChar = (inputChar - mix + 256) % 256; // Ensure positive by adding 256 before mod
            }
            previousChar = inputChar; // Use current char as previous for next iteration
            char outputChar = (char) inputChar;
            output.append(outputChar);
        }


        // Further scramble the output using a simple transposition based on key and IV length for more complexity
        if (encrypt) {
            output = new StringBuilder(scramble(output.toString(), KEY.length() + IV.length()));
        } else {
            output = new StringBuilder(unscramble(output.toString(), KEY.length() + IV.length()));
        }

        return output.toString();
    }

    private static String scramble(String input, int step) {
        char[] arr = new char[input.length()];
        for (int i = 0; i < input.length(); i++) {
            int j = (i * step) % input.length();
            arr[j] = input.charAt(i);
        }
        return new String(arr);
    }

    private static String unscramble(String input, int step) {
        char[] arr = new char[input.length()];
        for (int i = 0; i < input.length(); i++) {
            int j = (i * step) % input.length();
            arr[i] = input.charAt(j);
        }
        return new String(arr);
    }

    @Override
    public byte[] encrypt(String input, int key) {
        try {
            return transform(input, true).getBytes(StandardCharsets.UTF_8);
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed to encrypt", ex);
        }
    }

    @Override
    public String decrypt(byte[] input, int key) {
        try {
            return transform(new String(input, StandardCharsets.UTF_8), false);
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed to decrypt", ex);
        }
    }

    @Override
    public void visit(final SkidClassNode node, String name) {

    }
}
