package dev.skidfuscator.testclasses.conditionals;


import dev.skidfuscator.testclasses.TestRun;

public class Ifacmpne implements TestRun {
    @Override
    public void run() {
        assert exec(null, null) : "Failed null check";
        assert !exec(null, "") : "Failed assert check";
        //assert !exec(" ", " ") : "Failed constant pool check";
    }

    public boolean exec(Object value0, Object value1) {
        return value0 == value1;
    }

}
