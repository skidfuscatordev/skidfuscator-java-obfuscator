package dev.skidfuscator.testclasses.java_lang_String;

import dev.skidfuscator.testclasses.TestRun;

public class EqualsTestClass implements TestRun {
    @Override
    public void run() {
        assert "I like happy meals".equals(getReal()) : "Failed String test equality";
        assert "I like Happy Meals".equalsIgnoreCase(getReal()) : "Failed String test equality";
    }

    public String getReal() {
        return "I like happy meals";
    }
}
