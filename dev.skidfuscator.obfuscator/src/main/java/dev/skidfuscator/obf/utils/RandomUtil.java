package dev.skidfuscator.obf.utils;

import lombok.experimental.UtilityClass;

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
}
