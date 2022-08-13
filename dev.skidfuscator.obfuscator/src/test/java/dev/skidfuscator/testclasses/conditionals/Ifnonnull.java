package dev.skidfuscator.testclasses.conditionals;


import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

public class Ifnonnull implements TestRun {
    @Override
    public void run() {
        assert exec(null) == exec_real(null) : "Failed equality";
        assert exec("") == exec_real("") : "Failed invert";
    }

    public boolean exec(Object value) {
        return value == null;
    }
    @Exclude
    public boolean exec_real(Object value) {
        return value == null;
    }

}
