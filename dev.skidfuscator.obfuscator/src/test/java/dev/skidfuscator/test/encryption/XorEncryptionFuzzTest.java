package dev.skidfuscator.test.encryption;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class XorEncryptionFuzzTest {

    @Test
    public void test() {
        final String string = "tes68zge4896gzer74gezr974gzre$^mù'$ù$*ù*ù$^$ùm*ù987zrg789gz798t";
        final int key = 1645641651;
        final String encrypted = factor(string, key);
        final String decrypted = factor(encrypted, key);

        if (!decrypted.equals(string)) {
            throw new IllegalStateException("Decrypted: " + decrypted);
        }
    }

    public static String factor(final String string, int key) {
        final byte[] encrypted = string.getBytes();
        final byte[] modulo = Integer.toString(key).getBytes();

        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] = (byte) (encrypted[i] ^ modulo[i % modulo.length]);
        }

        return new String(encrypted);
    }

}
