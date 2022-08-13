package dev.skidfuscator.testclasses.conditionals;


import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

public class TableswitchNoDefault implements TestRun {
    @Override
    public void run() {
        for (char i = 0; i < 16; i++) {
            assert exec(i) == exec_real(i) : "Failed equality check";
        }
    }

    public int exec(char a) {
        int retval = 1;
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
        }
        return retval;
    }

    @Exclude
    public int exec_real(char a) {
        int retval = 1;
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
        }
        return retval;
    }

}
