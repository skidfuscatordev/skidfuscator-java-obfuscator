package dev.skidfuscator.testclasses.conditionals;


import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

import java.util.Random;

public class Ificmpgt implements TestRun {
    @Override
    public void run() {
        final Random random = new Random();
        for (int i = 0; i < 64; i++) {
            final int value = random.nextInt(Integer.MAX_VALUE);
            assert exec(value) == exec_real(value) : "Failed equality check";
        }
    }

    public boolean exec(int value) {
        return value <= 1000000000;
    }
    @Exclude
    public boolean exec_real(int value) {
        return value <= 1000000000;
    }
}
