package dev.skidfuscator.test.encryption;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class XorEncryptionFuzzTest {

    //@Test
    public void test() {
        final String string = "tes68zge4896gzer74gezr974gzre$^mù'$ù$*ù*ù$^$ùm*ù987zrg789gz798t";
        final int key = 1645641651;
        final String encrypted = factor(Base64.getEncoder().encodeToString(string.getBytes()), key);
        final String decrypted = new String(Base64.getDecoder().decode(factor(encrypted, key).getBytes()));

        if (!decrypted.equals(string)) {
            throw new IllegalStateException("Decrypted: " + decrypted);
        }
    }

    public static String factor(final String string, int key) {
        final byte[] encrypted = Base64.getDecoder().decode(string);
        final byte[] modulo = Integer.toString(key).getBytes();

        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] = (byte) (encrypted[i] ^ modulo[i % modulo.length]);
        }

        return Base64.getEncoder().encodeToString(encrypted);
    }

}
