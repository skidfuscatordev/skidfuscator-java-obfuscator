package dev.skidfuscator.core;

import dev.skidfuscator.testclasses.TestRun;

import java.util.List;
import java.util.Map;

public interface TestCase {
    Class<? extends TestRun> getMainClass();

    Class<?>[] getClasses();

    void receiveAndExecute(final List<Map.Entry<String, byte[]>> output);
}

