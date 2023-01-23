package dev.skidfuscator.test.stack;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;

public class Dup2_X1Test extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return dev.skidfuscator.testclasses.stack.Dup2_X1.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[0];
    }
}
