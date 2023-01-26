package dev.skidfuscator.testclasses.string;

import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;
import org.junit.jupiter.api.Assertions;

public class RegularStringTestClass implements TestRun {
    @Exclude
    @Override
    public void run() {
        System.out.println(get());
        System.out.println(getReal());

        assert get().equals(getReal()) : "Failed String test equality";
    }

    public String get() {
        return "I like happy meals";
    }

    @Exclude
    public String getReal() {
        return "I like happy meals";
    }
}
