package dev.skidfuscator.testclasses.conditionals;

import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

public class Tableswitch implements TestRun {
    @Override
    public void run() {
        System.out.println("Beginning test!");
        for (char i = 0; i < 16; i++) {
            System.out.println("Running exec " + (int) i);
            assert exec(i) == exec_real(i) : "Failed equality check";
        }
        System.out.println("Ending test!");
    }

    public int exec(char a) {
        int retval;
        switch (a) {
            case 10:
                retval = -1;
                break;
            case 11:
                retval = 2;
                break;
            case 13:
                retval = 3;
                break;
            case 12:
            case 17:
            case 16:
                retval = 4;
                break;
            case 15:
                retval = 0;
                break;
            default:
                retval = 5;
        }
        return retval;
    }

    @Exclude
    public int exec_real(char a) {
        int retval;
        switch (a) {
            case 10:
                retval = -1;
                break;
            case 11:
                retval = 2;
                break;
            case 13:
                retval = 3;
                break;
            case 12:
            case 17:
            case 16:
                retval = 4;
                break;
            case 15:
                retval = 0;
                break;
            default:
                retval = 5;
        }
        return retval;
    }

}
