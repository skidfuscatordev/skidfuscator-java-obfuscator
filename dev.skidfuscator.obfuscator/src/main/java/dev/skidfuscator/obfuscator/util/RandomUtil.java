package dev.skidfuscator.obfuscator.util;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.Random;

@UtilityClass
public class RandomUtil {
    private final Random random = new Random();

    public int nextInt() {
        return nextInt(Integer.MAX_VALUE);
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public long nextLong() {
        return random.nextLong();
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    public String randomIsoString(int size) {
        final int leftLimit = 48; // numeral '0'
        final int rightLimit = 122; // letter 'z'
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(size)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public String randomAlphabeticalString(int size) {
        final int leftLimit = 97; // letter 'a'
        final int rightLimit = 122; // letter 'z'

        return random.ints(leftLimit, rightLimit + 1)
                .limit(size)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private final Class<?>[] exceptionClasses = new Class<?>[] {
            IllegalAccessException.class,
            IOException.class,
            RuntimeException.class,
            ArrayStoreException.class
    };

    public static Type nextException() {
        return Type.getType(exceptionClasses[nextInt(exceptionClasses.length - 1)]);
    }
}
