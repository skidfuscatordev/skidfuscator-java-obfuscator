package dev.skidfuscator.testclasses.type;

import dev.skidfuscator.testclasses.TestRun;

public class InstanceOf implements TestRun {
    @Override
    public void run() {
        assert this instanceof Object : "Failed instance of test";
    }
}
