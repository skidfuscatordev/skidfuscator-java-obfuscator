package dev.skidfuscator.obf.utils;

import lombok.experimental.UtilityClass;

import java.util.Random;

@UtilityClass
public class RandomUtil {
    private final Random random = new Random();
    public int nextInt() {
        return random.nextInt(Integer.MAX_VALUE);
    }
}
