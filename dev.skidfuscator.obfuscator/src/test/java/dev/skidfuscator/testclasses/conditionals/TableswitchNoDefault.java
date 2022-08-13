package dev.skidfuscator.testclasses.conditionals;


public class TableswitchNoDefault {
    public TableswitchNoDefault() {

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

}
