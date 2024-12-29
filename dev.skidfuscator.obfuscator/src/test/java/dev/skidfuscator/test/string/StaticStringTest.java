package dev.skidfuscator.test.string;

import dev.skidfuscator.core.SkidTest;
import dev.skidfuscator.testclasses.TestRun;
import dev.skidfuscator.testclasses.string.RegularStringTestClass;
import dev.skidfuscator.testclasses.string.StaticStringTestClass;

public class StaticStringTest extends SkidTest {
    @Override
    public Class<? extends TestRun> getMainClass() {
        return StaticStringTestClass.class;
    }

    @Override
    public Class<?>[] getClasses() {
        return new Class[]{StaticStringTestClass.class};
    }
}
