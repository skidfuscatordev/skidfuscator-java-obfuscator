package dev.skidfuscator.testclasses.conditionals;


import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

import java.util.Random;

public class LookupswitchString implements TestRun {
    @Override
    public void run() {
        final Random random = new Random();
        final String[] nbs = new String[] {
                "hello", "world", "ciao", "mondo", "お早う", "お早う", "世界", "artemis"
        };
        for (int i = 0; i < 64; i++) {
            final String value = nbs[random.nextInt(nbs.length)];
            assert exec(value) == exec_real(value) : "Failed equality check";
        }
    }
    public int exec(String a) {
        int retval;
        switch (a) {
            case "hello":
                retval = -1;
                break;
            case "world":
                retval = 2;
                break;
            case "ciao":
                retval = 3;
                break;
            case "mondo":
            case "お早う":
            case "世界":
                retval = 4;
                break;
            default:
                retval = 5;
        }
        return retval;
    }

    @Exclude
    public int exec_real(String a) {
        int retval;
        switch (a) {
            case "hello":
                retval = -1;
                break;
            case "world":
                retval = 2;
                break;
            case "ciao":
                retval = 3;
                break;
            case "mondo":
            case "お早う":
            case "世界":
                retval = 4;
                break;
            default:
                retval = 5;
        }
        return retval;
    }

}
