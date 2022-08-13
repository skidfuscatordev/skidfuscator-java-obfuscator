package dev.skidfuscator.testclasses.conditionals;


import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

import java.util.Random;

public class Iflcmp implements TestRun {
    @Override
    public void run() {
        final Random random = new Random();
        for (int i = 0; i < 64; i++) {
            final long value = random.nextLong();
            assert exec(value) == exec_real(value) : "Failed equality check";
        }
    }

    public int exec(long value) {
        int a = 0, b = 0, c = 0, d = 0, e = 0, f = 0;
        if (value == 10000000000L)
            a = 1;
        if (value != 10000000000L)
            b = 10;
        if (value < 10000000000L)
            c = 100;
        if (value <= 10000000000L)
            d = 1000;
        if (value > 10000000000L)
            e = 10000;
        if (value >= 10000000000L)
            f = 100000;
        return a + b + c + d + e + f;
    }

    @Exclude
    public int exec_real(long value) {
        int a = 0, b = 0, c = 0, d = 0, e = 0, f = 0;
        if (value == 10000000000L)
            a = 1;
        if (value != 10000000000L)
            b = 10;
        if (value < 10000000000L)
            c = 100;
        if (value <= 10000000000L)
            d = 1000;
        if (value > 10000000000L)
            e = 10000;
        if (value >= 10000000000L)
            f = 100000;
        return a + b + c + d + e + f;
    }
}
