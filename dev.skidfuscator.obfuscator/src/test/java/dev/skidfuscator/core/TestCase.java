package dev.skidfuscator.core;

import dev.skidfuscator.testclasses.TestRun;

import java.util.Map;

public interface TestCase {
    Class<? extends TestRun> getMainClass();

    Class<?>[] getClasses();

    void receiveAndExecute(final Map<String, byte[]> output);
}

