package dev.skidfuscator.testclasses.conditionals;


public class Tableswitch {
    public Tableswitch() {

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

}
