package dev.skidfuscator.test.string;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.string.RegularStringTestClass;

public class RegularStringTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return RegularStringTestClass.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{RegularStringTestClass.class};
    }
}
