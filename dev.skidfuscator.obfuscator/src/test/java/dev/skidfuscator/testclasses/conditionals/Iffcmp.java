package dev.skidfuscator.testclasses.conditionals;


import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

import java.util.Random;

public class Iffcmp implements TestRun {
    @Override
    public void run() {
        final Random random = new Random();
        for (int i = 0; i < 64; i++) {
            final float randomF = random.nextFloat();
            assert exec(randomF) == exec_real(randomF) : "Failed equality check";
        }
    }

    public int exec(float value) {
        int a = 0, b = 0, c = 0, d = 0, e = 0, f = 0;
        float compareme = 3.5f;
        if (value == compareme)
            a = 1;
        if (value != compareme)
            b = 10;
        if (value < compareme)
            c = 100;
        if (value <= compareme)
            d = 1000;
        if (value > compareme)
            e = 10000;
        if (value >= compareme)
            f = 100000;
        return a + b + c + d + e + f;
    }

    @Exclude
    public int exec_real(float value) {
        int a = 0, b = 0, c = 0, d = 0, e = 0, f = 0;
        float compareme = 3.5f;
        if (value == compareme)
            a = 1;
        if (value != compareme)
            b = 10;
        if (value < compareme)
            c = 100;
        if (value <= compareme)
            d = 1000;
        if (value > compareme)
            e = 10000;
        if (value >= compareme)
            f = 100000;
        return a + b + c + d + e + f;
    }

}
