package dev.skidfuscator.test.type;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.type.InstanceOf;

public class InstanceOfTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return InstanceOf.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{
                InstanceOf.class
        };
    }
}
