package dev.skidfuscator.testclasses.conditionals;


import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

import java.util.Random;

public class Lookupswitch implements TestRun {
    @Override
    public void run() {
        final Random random = new Random();
        final int[] nbs = new int[] {
                -5000, 17, 1, 1000000, 3, -200, 299
        };
        for (int i = 0; i < 64; i++) {
            final int value = nbs[random.nextInt(nbs.length)];
            assert exec(value) == exec_real(value) : "Failed equality check";
        }
    }

    public int exec(int a) {
        int retval;
        switch (a) {
            case -5000:
                retval = -1;
                break;
            case 17:
                retval = 2;
                break;
            case 1:
                retval = 3;
                break;
            case 1000000:
            case 3:
            case -200:
                retval = 4;
                break;
            default:
                retval = 5;
        }
        return retval;
    }

    @Exclude
    public int exec_real(int a) {
        int retval;
        switch (a) {
            case -5000:
                retval = -1;
                break;
            case 17:
                retval = 2;
                break;
            case 1:
                retval = 3;
                break;
            case 1000000:
            case 3:
            case -200:
                retval = 4;
                break;
            default:
                retval = 5;
        }
        return retval;
    }

}
