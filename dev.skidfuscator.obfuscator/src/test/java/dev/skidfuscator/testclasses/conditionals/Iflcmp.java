package dev.skidfuscator.testclasses.conditionals;


public class Iflcmp {
    public Iflcmp() {

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

}
