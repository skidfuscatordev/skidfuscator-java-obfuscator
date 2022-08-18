package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.transform.impl.string.generator.BasicEncryptionGenerator;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import org.junit.jupiter.api.Test;

public class EncryptionTest {

    @Test
    public void simpleStringEncryptTest() {
        final String string = RandomUtil.randomAlphabeticalString(10);

        final Integer[] keysT = this._genKeys();

        final BasicEncryptionGenerator generator = new BasicEncryptionGenerator(keysT);
        final int seed = this._genSeed();

        final String encrypted = generator.encrypt(string, seed);
        final String decrypted = generator.decrypt(encrypted, seed);

        assert string.equals(decrypted) : "Encrypted string failed: " + string + " became " + decrypted;
        System.out.println("Passed Encryption Test #1");
    }

    @Test
    public void simpleStringEncryptTestUTF8() {
        final String string = "Œüèé€ìàò";

        final Integer[] keysT = this._genKeys();
        final BasicEncryptionGenerator generator = new BasicEncryptionGenerator(keysT);
        final int seed = this._genSeed();

        final String encrypted = generator.encrypt(string, seed);
        final String decrypted = generator.decrypt(encrypted, seed);

        assert string.equals(decrypted) : "Encrypted string failed: " + string + " became " + decrypted;
        System.out.println("Passed Encryption Test #2");
    }

    private int _genSeed() {
        return RandomUtil.nextInt();
    }

    private Integer[] _genKeys() {
        final int size = RandomUtil.nextInt(128) + 1;
        final Integer[] keysT = new Integer[size];
        for (int i = 0; i < size; i++) {
            keysT[i] = RandomUtil.nextInt(128);
        }

        return keysT;
    }
}
