package dev.skidfuscator.testclasses.string;

import dev.skidfuscator.annotations.Exclude;
import dev.skidfuscator.testclasses.TestRun;

public class StaticStringTestClass implements TestRun {
    @Override
    public void run() {
        System.out.println(get());
        System.out.println(getReal());
        System.out.println("I like happy meals".equals(getReal()));

        assert get().equals(getReal()) : "Failed String test equality";
        throw new IllegalStateException("This is a test");
    }

    public String get() {
        return "I like happy meals";
    }

    @Exclude
    public String getReal() {
        return "I like happy meals";
    }
}
